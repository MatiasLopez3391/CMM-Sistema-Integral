package ar.org.cmm;

import ar.org.cmm.cobertura.CoseguroServicioImpl;
import ar.org.cmm.cobranza.CobranzaDtos.LoteDto;
import ar.org.cmm.cobranza.CobranzaService;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.CoseguroServicio.CalculoCoseguro;
import ar.org.cmm.contratos.CuentaCorrienteServicio;
import ar.org.cmm.facturacion.ExportacionAsientosService;
import ar.org.cmm.facturacion.FacturacionDtos.MorosoDto;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoGeneracion;
import ar.org.cmm.facturacion.GeneracionCuotasService;
import ar.org.cmm.facturacion.ReportesFacturacionService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ciclo completo del Sprint 2 contra la base (H2 en modo MySQL con las migraciones reales):
 * cuota → pago por archivo → recargo por mora → morosos → asientos FEBBO. También valida la regla de coseguro (H12).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlujoFacturacionTest {

    @Autowired GeneracionCuotasService generacion;
    @Autowired CobranzaService cobranza;
    @Autowired CuentaCorrienteServicio cuentaCorriente;
    @Autowired ReportesFacturacionService reportes;
    @Autowired ExportacionAsientosService exportacion;
    @Autowired AfiliadoConsulta afiliados;
    @Autowired CoseguroServicioImpl coseguro;

    @Test
    @Order(1)
    void coseguroSegunCoberturaDelPlan() {
        // Consulta con especialista, Clínica Santa Rosa, agosto 2026: $28.118,34
        List<CalculoCoseguro> tabla = coseguro.simularTodosLosPlanes(1L, 4L, LocalDate.of(2026, 8, 15));
        assertThat(tabla).filteredOn(c -> c.planCodigo().equals("0101")).singleElement()
                .satisfies(c -> assertThat(c.coseguroAfiliado()).isEqualByComparingTo("0.00"));
        assertThat(tabla).filteredOn(c -> c.planCodigo().equals("0104")).singleElement()
                .satisfies(c -> assertThat(c.coseguroAfiliado()).isEqualByComparingTo("5623.67"));
        assertThat(tabla).filteredOn(c -> c.planCodigo().equals("0106")).singleElement()
                .satisfies(c -> assertThat(c.coseguroAfiliado()).isEqualByComparingTo("8435.50"));
    }

    @Test
    @Order(2)
    void generaCuotasConIntegrantesYAdicionalesYEsIdempotente() {
        ResultadoGeneracion r = generacion.generar("202609", LocalDate.of(2026, 9, 1));
        assertThat(r.generados()).isEqualTo(11); // 11 titulares activos (el dado de baja no se factura)
        // Grupo GÓMEZ: plan 0104 ($228.990) × 3 integrantes + subsidio $1.878 × 3
        assertThat(r.comprobantes()).filteredOn(p -> p.nroAfiliado().equals("131090000100")).singleElement()
                .satisfies(p -> assertThat(p.total()).isEqualByComparingTo("692604.00"));
        ResultadoGeneracion otra = generacion.generar("202609", LocalDate.of(2026, 9, 1));
        assertThat(otra.generados()).isZero();
        assertThat(otra.omitidosYaExistian()).isEqualTo(11);
    }

    @Test
    @Order(3)
    void importaArchivoRechazaInvalidosEImputaPagos() {
        String txt = "D13109000010020260909000069260400PMC202609000001\n"
                + "D13109000080020260909000010000000PMC202609000002\n"
                + "D13109999990020260909000001500000PMC202609000003\n";
        LoteDto lote = cobranza.previsualizar(1L, new MockMultipartFile("archivo", "pmc.txt", "text/plain", txt.getBytes(StandardCharsets.UTF_8)));
        assertThat(lote.cantidadOk()).isEqualTo(2);
        assertThat(lote.cantidadRechazados()).isEqualTo(1);
        cobranza.confirmar(lote.id());

        Long gomez = afiliados.porNroAfiliado("131090000100").orElseThrow().id();
        Long ortiz = afiliados.porNroAfiliado("131090000800").orElseThrow().id();
        assertThat(cuentaCorriente.saldo(gomez)).isEqualByComparingTo("0.00");
        assertThat(cuentaCorriente.saldo(ortiz)).isEqualByComparingTo("82404.00"); // 182.404 - 100.000

        assertThatThrownBy(() -> cobranza.previsualizar(1L,
                new MockMultipartFile("archivo", "pmc.txt", "text/plain", txt.getBytes(StandardCharsets.UTF_8))))
                .hasMessageContaining("ya fue importado");
    }

    @Test
    @Order(4)
    void aplicaRecargoSobreDeudaVencidaYListaMorosos() {
        ResultadoGeneracion r = generacion.generar("202610", LocalDate.of(2026, 10, 1));
        // ORTIZ adeuda $82.404 vencidos: recargo 1,40% = $1.153,66
        assertThat(r.comprobantes()).filteredOn(p -> p.nroAfiliado().equals("131090000800")).singleElement()
                .satisfies(p -> assertThat(p.recargo()).isEqualByComparingTo("1153.66"));
        List<MorosoDto> morosos = reportes.morosos(LocalDate.of(2026, 10, 5)); // antes del vencimiento de octubre
        assertThat(morosos).extracting(MorosoDto::nroAfiliado).contains("131090000800").doesNotContain("131090000100");
    }

    @Test
    @Order(5)
    void exportaAsientosBalanceados() {
        ExportacionAsientosService.Exportacion e = exportacion.exportar(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31));
        assertThat(e.asientos()).isGreaterThan(0);
        assertThat(e.totalDebe()).isEqualByComparingTo(e.totalHaber());
        assertThat(e.contenido()).startsWith("nro_asiento;fecha;cuenta;debe;haber;concepto;referencia");
        assertThat(e.totalDebe()).isGreaterThan(BigDecimal.ZERO);
    }
}
