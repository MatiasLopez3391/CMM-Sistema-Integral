package ar.org.cmm.contratos;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrato publicado por F3 (Cuotas). F5 (Órdenes) lo llama al emitir una orden para que su coseguro
 * se cobre en la cuota del mes siguiente, a nombre del integrante que la usó.
 *
 * <p>El coseguro se registra al EMITIR la orden: si el afiliado no la usa y no la anula, se cobra igual
 * (respuesta M7 del CMM).
 */
public interface CargoCuotaServicio {

    /**
     * Registra el coseguro de una orden. Es idempotente por {@code origenRef}: si la orden ya tiene su cargo,
     * devuelve el id existente sin duplicarlo.
     *
     * @return id del cargo
     */
    Long registrarCoseguro(CoseguroParaCuota coseguro);

    /**
     * Anula el coseguro de una orden anulada. Solo se puede mientras no se haya facturado; si ya está en una
     * cuota, la corrección se hace con una nota de crédito.
     */
    void anularCoseguro(String origenRef, String motivo);

    /**
     * @param afiliadoId integrante que usó la orden (puede ser el titular o un familiar)
     * @param fecha      fecha de emisión de la orden; el cargo va a la cuota del mes siguiente
     * @param concepto   texto que verá el afiliado en la cuota (ej. "Consulta especialista - Clínica Santa Rosa")
     * @param origenRef  número de la orden
     */
    record CoseguroParaCuota(Long afiliadoId, LocalDate fecha, BigDecimal importe, String concepto, String origenRef) {
    }
}
