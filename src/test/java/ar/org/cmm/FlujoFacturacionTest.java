package ar.org.cmm;

import ar.org.cmm.cobertura.CoseguroServicioImpl;
import ar.org.cmm.cobranza.CobranzaDtos.LoteDto;
import ar.org.cmm.cobranza.CobranzaService;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.CargoCuotaServicio;
import ar.org.cmm.contratos.CargoCuotaServicio.CoseguroParaCuota;
import ar.org.cmm.facturacion.CargoCuotaService;
import ar.org.cmm.facturacion.FacturacionDtos.AplicarIpcRequest;
import ar.org.cmm.facturacion.FacturacionDtos.CargoManualRequest;
import ar.org.cmm.facturacion.FacturacionDtos.IpcRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoActualizacion;
import ar.org.cmm.facturacion.IpcService;
import ar.org.cmm.facturacion.CodigoServicioRepository;
import ar.org.cmm.facturacion.ComprobanteRepository;
import ar.org.cmm.facturacion.NotaService;
import ar.org.cmm.facturacion.FacturacionDtos.NotaRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoNota;
import ar.org.cmm.facturacion.ValorCuotaService;
import ar.org.cmm.facturacion.FacturacionDtos.HistorialCosegurosDto;
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
    @Autowired CargoCuotaServicio cargosContrato;
    @Autowired CargoCuotaService cargos;
    @Autowired IpcService ipc;
    @Autowired ValorCuotaService valores;
    @Autowired NotaService notas;
    @Autowired ComprobanteRepository comprobantes;
    @Autowired CodigoServicioRepository codigos;

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
        // ORTIZ adeuda $82.404 vencidos: recargo con la tasa activa del BNA, 4,20% = $3.460,97
        assertThat(r.comprobantes()).filteredOn(p -> p.nroAfiliado().equals("131090000800")).singleElement()
                .satisfies(p -> assertThat(p.recargo()).isEqualByComparingTo("3460.97"));
        // El 10/10/2026 es sábado y el lunes 12 es feriado: la cuota de octubre vence el martes 13
        assertThat(r.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 10, 13));
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

    @Test
    @Order(6)
    void cosegurosDeLasOrdenesSeSumanALaCuotaSiguiente() {
        Long gomez = afiliados.porNroAfiliado("131090000100").orElseThrow().id();
        Long hijo = afiliados.porNroAfiliado("131090000101").orElseThrow().id();
        // F5 emite una orden en octubre: el coseguro va a la cuota de noviembre a nombre del integrante
        Long id = cargosContrato.registrarCoseguro(new CoseguroParaCuota(hijo, LocalDate.of(2026, 10, 15),
                new BigDecimal("5623.67"), "Consulta con especialista - Clínica Santa Rosa", "ORD-0001"));
        // Idempotente: reintentar la misma orden no duplica el cargo
        assertThat(cargosContrato.registrarCoseguro(new CoseguroParaCuota(hijo, LocalDate.of(2026, 10, 15),
                new BigDecimal("5623.67"), "Consulta con especialista - Clínica Santa Rosa", "ORD-0001"))).isEqualTo(id);
        // Carga manual de Liquidaciones y una orden anulada antes de facturarse
        cargos.cargarManual(new CargoManualRequest(gomez, LocalDate.of(2026, 10, 20), new BigDecimal("3480"), "Consulta - Clínica Santa Rosa", null));
        cargosContrato.registrarCoseguro(new CoseguroParaCuota(hijo, LocalDate.of(2026, 10, 22), new BigDecimal("999"), "Orden anulada", "ORD-0002"));
        cargosContrato.anularCoseguro("ORD-0002", "El afiliado anuló la orden");

        ResultadoGeneracion r = generacion.generar("202611", LocalDate.of(2026, 11, 1));
        assertThat(r.totalCoseguros()).isEqualByComparingTo("9103.67");
        assertThat(r.comprobantes()).filteredOn(p -> p.nroAfiliado().equals("131090000100")).singleElement()
                .satisfies(p -> {
                    assertThat(p.coseguros()).isEqualByComparingTo("9103.67");
                    assertThat(p.cantidadCoseguros()).isEqualTo(2);
                });

        HistorialCosegurosDto h = cargos.historial("202611", "202611", null, gomez);
        assertThat(h.totalFacturado()).isEqualByComparingTo("9103.67");
        assertThat(h.totalPendiente()).isEqualByComparingTo("0");
        assertThat(h.cargos()).filteredOn(c -> c.estado().equals("ANULADO")).hasSize(1);
        assertThat(h.cargos()).filteredOn(c -> c.estado().equals("FACTURADO")).allSatisfy(c -> assertThat(c.comprobanteNumero()).startsWith("CI-"));

        // Ya facturado: no se puede anular (se corrige con nota de crédito)
        assertThatThrownBy(() -> cargosContrato.anularCoseguro("ORD-0001", "error")).hasMessageContaining("nota de crédito");
    }

    @Test
    @Order(7)
    void elIpcCargadoSeAplicaALosValoresDeCuotaUnaSolaVez() {
        ipc.cargarManual(new IpcRequest("2026-11", new BigDecimal("2.5")));
        Long plan0104 = afiliados.porNroAfiliado("131090000100").orElseThrow().planId();
        BigDecimal antes = valores.vigente(plan0104, LocalDate.of(2026, 12, 31)).orElseThrow();
        ResultadoActualizacion r = ipc.aplicar("202611", new AplicarIpcRequest(LocalDate.of(2027, 1, 1), List.of(plan0104)));
        assertThat(r.planesActualizados()).isEqualTo(1);
        assertThat(valores.vigente(plan0104, LocalDate.of(2027, 1, 1)).orElseThrow())
                .isEqualByComparingTo(antes.multiply(new BigDecimal("1.025")).setScale(2, java.math.RoundingMode.HALF_UP));
        assertThat(ipc.ultimos()).filteredOn(i -> i.periodo().equals("202611")).singleElement()
                .satisfies(i -> assertThat(i.aplicadoVigencia()).isEqualTo(LocalDate.of(2027, 1, 1)));
        assertThatThrownBy(() -> ipc.aplicar("202611", new AplicarIpcRequest(LocalDate.of(2027, 2, 1), null)))
                .hasMessageContaining("ya se aplicó");
        assertThatThrownBy(() -> ipc.cargarManual(new IpcRequest("202611", new BigDecimal("3")))).hasMessageContaining("ya se aplicó");
    }

    @Test
    @Order(8)
    void notasDeCreditoYDebitoCorrigenLaCuentaCorriente() {
        Long ortiz = afiliados.porNroAfiliado("131090000800").orElseThrow().id();
        Long gomez = afiliados.porNroAfiliado("131090000100").orElseThrow().id();
        Long recargo = codigos.findFirstByTipoAndActivoTrue("RECARGO").orElseThrow().getId();
        Long cuota = codigos.findFirstByTipoAndActivoTrue("CUOTA").orElseThrow().getId();
        var octubre = comprobantes.findByAfiliadoIdOrderByFechaEmisionDesc(ortiz).stream()
                .filter(c -> c.getPeriodo().equals("202610") && c.getTipo().equals("CUOTA")).findFirst().orElseThrow();
        BigDecimal saldoAntes = cuentaCorriente.saldo(ortiz);
        BigDecimal saldoCuotaAntes = octubre.getSaldo();

        // Recargo mal cobrado en octubre: nota de crédito sobre ese comprobante
        ResultadoNota nc = notas.emitir(new NotaRequest("CREDITO", ortiz, LocalDate.of(2026, 11, 20), recargo,
                new BigDecimal("3460.97"), "Recargo mal aplicado", octubre.getId()));
        assertThat(nc.numero()).startsWith("NC-");
        assertThat(nc.aplicado()).isEqualByComparingTo("3460.97");
        assertThat(cuentaCorriente.saldo(ortiz)).isEqualByComparingTo(saldoAntes.subtract(new BigDecimal("3460.97")));
        assertThat(comprobantes.findById(octubre.getId()).orElseThrow().getSaldo())
                .isEqualByComparingTo(saldoCuotaAntes.subtract(new BigDecimal("3460.97")));
        // No se puede acreditar más de lo que tiene el comprobante
        assertThatThrownBy(() -> notas.emitir(new NotaRequest("CREDITO", ortiz, LocalDate.of(2026, 11, 20), cuota,
                octubre.getTotal(), "Error", octubre.getId()))).hasMessageContaining("supera");

        // Nota de débito: suma deuda y vence con la próxima cuota (10/12/2026, jueves hábil)
        BigDecimal saldoGomez = cuentaCorriente.saldo(gomez);
        ResultadoNota nd = notas.emitir(new NotaRequest("DEBITO", gomez, LocalDate.of(2026, 11, 20), cuota,
                new BigDecimal("1500"), "Diferencia de cuota de noviembre", null));
        assertThat(nd.numero()).startsWith("ND-");
        assertThat(nd.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(cuentaCorriente.saldo(gomez)).isEqualByComparingTo(saldoGomez.add(new BigDecimal("1500")));

        // Los asientos del mes siguen balanceados con la nota de crédito (asiento inverso)
        ExportacionAsientosService.Exportacion e = exportacion.exportar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertThat(e.totalDebe()).isEqualByComparingTo(e.totalHaber());
        assertThat(e.contenido()).contains("Nota de crédito " + nc.numero()).contains("Nota de débito " + nd.numero());
    }
}
