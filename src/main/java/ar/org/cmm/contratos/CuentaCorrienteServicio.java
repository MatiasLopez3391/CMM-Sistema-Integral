package ar.org.cmm.contratos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrato publicado por F3 (Cuotas y Cuenta Corriente). F4 (Cobranza) lo llama para
 * impactar cada pago: genera el movimiento al Haber y lo imputa a la deuda más antigua.
 */
public interface CuentaCorrienteServicio {

    ResultadoImputacion registrarPago(PagoParaImputar pago);

    /** Saldo del grupo (positivo = debe). Se informa siempre sobre el titular. */
    BigDecimal saldo(Long afiliadoId);

    BigDecimal deudaVencida(Long afiliadoId, LocalDate alDia);

    /**
     * @param cuentaContable código de la cuenta contable del medio de cobro (para el asiento de FEBBO)
     */
    record PagoParaImputar(Long pagoId, Long afiliadoId, LocalDate fecha, BigDecimal importe, String concepto,
                           String cuentaContable) {
    }

    record ResultadoImputacion(Long titularId, BigDecimal importe, BigDecimal imputado, BigDecimal aCuenta,
                               List<String> comprobantesCancelados) {
    }
}
