package ar.org.cmm.contratos;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrato publicado por F2 (Cobertura). Regla acordada (H12):
 * coseguro = valor convenido vigente de la práctica × (1 − % de cobertura del plan para el rubro).
 */
public interface CoseguroServicio {

    CalculoCoseguro calcular(Long prestadorId, Long practicaId, Long planId, LocalDate fecha);

    /** Porcentaje de cobertura del plan para un rubro (aplica excepciones por rubro si existen). */
    BigDecimal porcentajeCobertura(Long planId, String rubro);

    record CalculoCoseguro(
            Long prestadorId,
            Long practicaId,
            Long planId,
            String planCodigo,
            String planNombre,
            String rubro,
            LocalDate valorVigenteDesde,
            BigDecimal valorConvenido,
            BigDecimal porcentajeCobertura,
            BigDecimal aCargoEntidad,
            BigDecimal coseguroAfiliado) {
    }
}
