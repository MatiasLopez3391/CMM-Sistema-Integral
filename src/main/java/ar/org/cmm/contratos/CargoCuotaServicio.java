package ar.org.cmm.contratos;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrato publicado por F3 (Cuotas). F5 (Órdenes) lo llama para que el coseguro de una orden se cobre en la cuota
 * del mes siguiente, a nombre del integrante que la usó.
 *
 * <p>El coseguro se registra cuando la orden queda válida: al emitirla, si no va a auditoría, o cuando el médico
 * auditor la autoriza, con el importe final. Una orden pendiente de auditoría no genera cargo. Si el afiliado no la
 * usa y no la anula, se cobra igual (respuesta M7 del CMM).
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
     * Anula el coseguro de una orden anulada o rechazada. No lanza error si la orden no tiene cargo o si ya está
     * anulado. Si el coseguro ya se facturó en una cuota no se toca: devuelve {@link ResultadoAnulacion#YA_FACTURADO}
     * con el período, y la devolución se hace con una nota de crédito desde Liquidaciones.
     */
    AnulacionCoseguro anularCoseguro(String origenRef, String motivo);

    /**
     * @param afiliadoId integrante que usó la orden (puede ser el titular o un familiar)
     * @param fecha      fecha en que la orden quedó válida; el cargo va a la cuota del mes siguiente
     * @param concepto   texto que verá el afiliado en la cuota (ej. "Consulta especialista - Clínica Santa Rosa")
     * @param origenRef  número de la orden
     */
    record CoseguroParaCuota(Long afiliadoId, LocalDate fecha, BigDecimal importe, String concepto, String origenRef) {
    }

    enum ResultadoAnulacion { ANULADO, YA_ANULADO, SIN_CARGO, YA_FACTURADO }

    /** @param periodoCuota cuota (AAAAMM) a la que iba o en la que se facturó el coseguro; null si no había cargo */
    record AnulacionCoseguro(ResultadoAnulacion resultado, String periodoCuota) {
    }
}
