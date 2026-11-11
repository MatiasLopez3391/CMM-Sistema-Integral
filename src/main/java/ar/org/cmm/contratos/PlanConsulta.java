package ar.org.cmm.contratos;

import java.math.BigDecimal;
import java.util.List;

/** Contrato publicado por F2 para leer planes sin depender del módulo Cobertura. */
public interface PlanConsulta {

    List<PlanResumen> todos();

    PlanResumen obtener(Long planId);

    record PlanResumen(Long id, String codigo, String nombre, BigDecimal coberturaPorcentaje, boolean activo) {
    }
}
