package ar.org.cmm.cobertura;

import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.CoseguroServicio;
import ar.org.cmm.contratos.CoseguroServicio.CalculoCoseguro;
import ar.org.cmm.contratos.PlanConsulta;
import ar.org.cmm.contratos.PracticaConsulta;
import ar.org.cmm.contratos.PrestadorConsulta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de integración de los contratos de F2 contra la base de pruebas (H2 + migraciones + datos demo).
 * Usa solo las interfaces de contratos, igual que lo harán los demás frentes.
 */
@SpringBootTest
@ActiveProfiles("test")
class CoseguroServicioTest {

    @Autowired CoseguroServicio coseguro;
    @Autowired PlanConsulta planes;
    @Autowired PracticaConsulta practicas;
    @Autowired PrestadorConsulta prestadores;

    private Long planId(String codigo) {
        return planes.todos().stream().filter(p -> p.codigo().equals(codigo)).findFirst().orElseThrow().id();
    }

    private Long practicaId(String codigoCmm) {
        return practicas.porCodigoCmm(codigoCmm).orElseThrow().id();
    }

    private Long santaRosaId() {
        return prestadores.todos().stream().filter(p -> p.nombre().equals("Clínica Santa Rosa")).findFirst().orElseThrow().id();
    }

    @Test
    void usaElValorConvenidoVigenteALaFechaConsultada() {
        // Julio 2026: $27.540,00 — plan 0104 (80/100) paga el 20%
        CalculoCoseguro julio = coseguro.calcular(santaRosaId(), practicaId("429019"), planId("0104"), LocalDate.of(2026, 7, 15));
        assertThat(julio.valorConvenido()).isEqualByComparingTo("27540.00");
        assertThat(julio.coseguroAfiliado()).isEqualByComparingTo("5508.00");

        // Agosto 2026: $28.118,34
        CalculoCoseguro agosto = coseguro.calcular(santaRosaId(), practicaId("429019"), planId("0104"), LocalDate.of(2026, 8, 15));
        assertThat(agosto.valorConvenido()).isEqualByComparingTo("28118.34");
        assertThat(agosto.coseguroAfiliado()).isEqualByComparingTo("5623.67");
    }

    @Test
    void aplicaLaExcepcionDeMedicamentosDelPlan4502() {
        Long plan = planId("4502");
        assertThat(coseguro.porcentajeCobertura(plan, "MEDICAMENTOS")).isEqualByComparingTo("40");
        assertThat(coseguro.porcentajeCobertura(plan, "CONSULTA")).isEqualByComparingTo("80");

        CalculoCoseguro c = coseguro.calcular(santaRosaId(), practicaId("MED001"), plan, LocalDate.of(2026, 8, 15));
        assertThat(c.coseguroAfiliado()).isEqualByComparingTo("30000.00"); // $50.000 al 40% de cobertura
    }

    @Test
    void elSimuladorDevuelveUnaFilaPorPlanActivo() {
        var tabla = coseguro.simularTodosLosPlanes(santaRosaId(), practicaId("429019"), LocalDate.of(2026, 8, 15));
        assertThat(tabla).extracting(CalculoCoseguro::planCodigo).contains("0101", "0104", "0106", "4502");
    }

    @Test
    void sinValorVigenteALaFechaInformaElErrorDeNegocio() {
        assertThatThrownBy(() -> coseguro.calcular(santaRosaId(), practicaId("429019"), planId("0104"), LocalDate.of(2026, 6, 1)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("valor convenido vigente");
    }

    @Test
    void losContratosDeConsultaExponenPracticasYPrestadores() {
        var consulta = practicas.porCodigoCmm("MED001").orElseThrow();
        assertThat(consulta.requiereAuditoria()).isTrue();
        assertThat(consulta.rubro()).isEqualTo("MEDICAMENTOS");
        assertThat(practicas.buscar("guardia")).extracting(PracticaConsulta.PracticaResumen::codigoCmm).contains("420103", "429010");
        assertThat(prestadores.todos()).extracting(PrestadorConsulta.PrestadorResumen::nombre).contains("Clínica Santa Rosa");
    }
}
