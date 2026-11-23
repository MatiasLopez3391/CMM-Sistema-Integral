package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.coseguro.CoberturaDePlan;
import ar.org.cmm.cobertura.coseguro.CoseguroPorCobertura;
import ar.org.cmm.contratos.CoseguroServicio.CalculoCoseguro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba unitaria (sin Spring ni base) de la regla H12: coseguro = valor × (1 − cobertura). */
class CoseguroPorCoberturaTest {

    private final CoseguroPorCobertura estrategia = new CoseguroPorCobertura(new CoberturaDePlan());

    @Test
    void calculaElCoseguroSegunLaCoberturaGeneralDelPlan() {
        Plan plan = plan("0104", "80.00");
        CalculoCoseguro c = estrategia.calcular(plan, practica("CONSULTA"), valor("28118.34"));
        assertThat(c.porcentajeCobertura()).isEqualByComparingTo("80");
        assertThat(c.aCargoEntidad()).isEqualByComparingTo("22494.67");
        assertThat(c.coseguroAfiliado()).isEqualByComparingTo("5623.67");
    }

    @Test
    void elPlanAl100PorCientoNoTieneCoseguro() {
        CalculoCoseguro c = estrategia.calcular(plan("0101", "100.00"), practica("CONSULTA"), valor("28118.34"));
        assertThat(c.coseguroAfiliado()).isEqualByComparingTo("0.00");
    }

    @Test
    void laExcepcionPorRubroTienePrioridadSobreLaCoberturaGeneral() {
        Plan plan = plan("4502", "80.00");
        CoberturaRubro excepcion = new CoberturaRubro();
        excepcion.setPlan(plan);
        excepcion.setRubro("MEDICAMENTOS");
        excepcion.setPorcentaje(new BigDecimal("40.00"));
        plan.getCoberturasPorRubro().add(excepcion);

        CalculoCoseguro medicamento = estrategia.calcular(plan, practica("MEDICAMENTOS"), valor("50000.00"));
        assertThat(medicamento.porcentajeCobertura()).isEqualByComparingTo("40");
        assertThat(medicamento.coseguroAfiliado()).isEqualByComparingTo("30000.00");

        CalculoCoseguro consulta = estrategia.calcular(plan, practica("CONSULTA"), valor("50000.00"));
        assertThat(consulta.porcentajeCobertura()).isEqualByComparingTo("80");
        assertThat(consulta.coseguroAfiliado()).isEqualByComparingTo("10000.00");
    }

    private static Plan plan(String codigo, String cobertura) {
        Plan p = new Plan();
        p.setId(1L);
        p.setCodigo(codigo);
        p.setNombre("Plan " + codigo);
        p.setCoberturaPorcentaje(new BigDecimal(cobertura));
        return p;
    }

    private static Practica practica(String rubro) {
        Practica p = new Practica();
        p.setId(1L);
        p.setCodigoCmm("X");
        p.setRubro(rubro);
        return p;
    }

    private static ValorConvenido valor(String importe) {
        Prestador prestador = new Prestador();
        prestador.setId(1L);
        ValorConvenido v = new ValorConvenido();
        v.setPrestador(prestador);
        v.setVigenteDesde(LocalDate.of(2026, 8, 1));
        v.setImporte(new BigDecimal(importe));
        return v;
    }
}
