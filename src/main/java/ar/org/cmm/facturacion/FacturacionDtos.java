package ar.org.cmm.facturacion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class FacturacionDtos {

    private FacturacionDtos() {
    }

    // ---------------- Valores de cuota
    public record ValorCuotaPlanDto(Long planId, String planCodigo, String planNombre, BigDecimal coberturaPorcentaje,
                                    LocalDate vigenteDesde, BigDecimal importeVigente, List<ValorHistoricoDto> historial) {
    }

    public record ValorHistoricoDto(Long id, LocalDate vigenteDesde, BigDecimal importe, String motivo, String usuario) {
    }

    public record ActualizacionCuotaRequest(@NotNull @DecimalMin(value = "-50") @DecimalMax("100") BigDecimal porcentaje,
                                            @NotNull LocalDate vigenteDesde,
                                            List<Long> planIds,
                                            @Size(max = 150) String motivo) {
    }

    public record ValorCuotaRequest(@NotNull Long planId, @NotNull LocalDate vigenteDesde, @NotNull @Positive BigDecimal importe,
                                    @Size(max = 150) String motivo) {
    }

    public record ResultadoActualizacion(int planesActualizados, List<String> detalle) {
    }

    // ---------------- Generación de cuotas
    public record GeneracionRequest(@NotBlank String periodo, LocalDate fechaEmision) {
    }

    public record PropuestaComprobante(Long afiliadoId, String nroAfiliado, String apellidoNombre, String planCodigo,
                                       int integrantes, BigDecimal cuota, BigDecimal adicionales, BigDecimal coseguros,
                                       int cantidadCoseguros, BigDecimal recargo, BigDecimal deudaVencida, BigDecimal total,
                                       String observacion) {
    }

    public record ResultadoGeneracion(String periodo, LocalDate fechaEmision, LocalDate fechaVencimiento, boolean simulacion,
                                      int generados, int omitidosYaExistian, int conRecargo,
                                      BigDecimal totalCuotas, BigDecimal totalCoseguros, BigDecimal totalRecargos,
                                      BigDecimal totalGeneral, List<String> advertencias, List<PropuestaComprobante> comprobantes) {
    }

    // ---------------- Coseguros a la cuota (T42, T43)
    public record CargoCuotaDto(Long id, Long titularId, String titularNro, String titularNombre, Long afiliadoId,
                                String afiliadoNro, String afiliadoNombre, String tipo, String concepto, LocalDate fecha,
                                BigDecimal importe, String periodoDestino, String origen, String origenRef, String estado,
                                Long comprobanteId, String comprobanteNumero, String usuario, String motivoAnulacion) {
    }

    public record CargoManualRequest(@NotNull Long afiliadoId, @NotNull LocalDate fecha, @NotNull @Positive BigDecimal importe,
                                     @NotBlank @Size(max = 200) String concepto, String periodoDestino) {
    }

    public record AnulacionRequest(@NotBlank @Size(max = 200) String motivo) {
    }

    public record CalculoCoseguroRequest(@NotNull Long afiliadoId, @NotNull Long prestadorId, @NotNull Long practicaId,
                                         LocalDate fecha) {
    }

    public record TotalPeriodoCosegurosDto(String periodo, int cantidad, BigDecimal pendiente, BigDecimal facturado,
                                           BigDecimal anulado) {
    }

    public record HistorialCosegurosDto(String desde, String hasta, List<CargoCuotaDto> cargos,
                                        List<TotalPeriodoCosegurosDto> porPeriodo, BigDecimal totalPendiente,
                                        BigDecimal totalFacturado) {
    }

    // ---------------- Comprobantes y cuenta corriente
    public record ComprobanteDto(Long id, String tipo, String numero, Long afiliadoId, String nroAfiliado, String apellidoNombre,
                                 String periodo, LocalDate fechaEmision, LocalDate fechaVencimiento, BigDecimal total,
                                 BigDecimal saldo, String estado, String observaciones, String comprobanteAsociado,
                                 List<ItemDto> items, List<ImputacionDto> imputaciones, List<NotaResumenDto> notas) {
    }

    /** Nota de crédito o débito emitida sobre un comprobante. */
    public record NotaResumenDto(Long id, String tipo, String numero, LocalDate fecha, BigDecimal importe, String motivo) {
    }

    public record ItemDto(String codigo, String descripcion, int cantidad, BigDecimal precioUnitario, BigDecimal importe,
                          String cuentaContable) {
    }

    /** Qué canceló el comprobante: un pago (pagoId) o una nota de crédito (origen = su número). */
    public record ImputacionDto(Long pagoId, LocalDate fecha, BigDecimal importe, String origen) {
    }

    public record MovimientoDto(Long id, LocalDate fecha, String concepto, BigDecimal debe, BigDecimal haber, BigDecimal saldo,
                                Long comprobanteId, Long pagoId) {
    }

    public record EstadoCuentaDto(Long titularId, String nroAfiliado, String apellidoNombre, String planCodigo, boolean bloqueado,
                                  LocalDate desde, LocalDate hasta, BigDecimal saldoInicial, List<MovimientoDto> movimientos,
                                  BigDecimal saldoFinal, BigDecimal deudaVencida, List<ComprobanteResumenDto> comprobantes) {
    }

    public record ComprobanteResumenDto(Long id, String tipo, String numero, String periodo, LocalDate fechaVencimiento,
                                        BigDecimal total, BigDecimal saldo, String estado) {
    }

    // ---------------- Morosos
    public record MorosoDto(Long afiliadoId, String nroAfiliado, String apellidoNombre, String planCodigo, String email,
                            int cuotasVencidas, BigDecimal deudaVencida, LocalDate vencimientoMasAntiguo, long diasAtraso,
                            boolean bloqueado) {
    }

    public record AvisoRequest(@NotNull List<Long> afiliadoIds) {
    }

    public record ResultadoAvisos(int enviados, int registradosSinSmtp, int sinEmail, int errores) {
    }

    // ---------------- Resumen de facturación
    public record ResumenFacturacionDto(String periodo, int comprobantes, BigDecimal total, BigDecimal cobrado, BigDecimal pendiente,
                                        BigDecimal porcentajeCobrado, List<ResumenConceptoDto> porConcepto,
                                        List<ResumenPlanDto> porPlan) {
    }

    public record ResumenConceptoDto(String codigo, String descripcion, String cuentaContable, long cantidad, BigDecimal importe) {
    }

    public record ResumenPlanDto(String planCodigo, String planNombre, int comprobantes, BigDecimal total, BigDecimal pendiente) {
    }

    // ---------------- Catálogos
    public record CodigoServicioDto(Long id, String codigo, String descripcion, String tipo, String cuentaCodigo,
                                    String cuentaNombre, BigDecimal importeFijo, boolean incluirEnCuota, boolean activo) {
    }

    public record CodigoServicioRequest(@NotNull Long cuentaContableId, BigDecimal importeFijo, boolean incluirEnCuota,
                                        boolean activo, @NotBlank @Size(max = 120) String descripcion) {
    }

    public record ParametroRequest(@NotBlank String valor) {
    }

    // ---------------- Calendario hábil e IPC (T44, T45)
    public record FeriadoDto(LocalDate fecha, String descripcion, String origen) {
    }

    public record FeriadoRequest(@NotNull LocalDate fecha, @NotBlank @Size(max = 150) String descripcion) {
    }

    public record ResultadoImportacionFeriados(int anio, int agregados, int yaExistian) {
    }

    public record IpcDto(String periodo, BigDecimal variacion, String origen, LocalDate aplicadoVigencia, String usuario) {
    }

    public record IpcRequest(@NotBlank String periodo, @NotNull @DecimalMin("-50") @DecimalMax("100") BigDecimal variacion) {
    }

    public record AplicarIpcRequest(@NotNull LocalDate vigenteDesde, List<Long> planIds) {
    }

    public record ResultadoImportacionIpc(int agregados, int yaExistian, String ultimoPeriodo) {
    }

    // ---------------- Notas de crédito y débito (T46)
    public record NotaRequest(@NotBlank String tipo, @NotNull Long afiliadoId, @NotNull LocalDate fecha, @NotNull Long codigoServicioId,
                              @NotNull @Positive BigDecimal importe, @NotBlank @Size(max = 200) String motivo,
                              Long comprobanteAsociadoId) {
    }

    public record ResultadoNota(Long id, String numero, String tipo, BigDecimal importe, BigDecimal aplicado, BigDecimal aFavor,
                                LocalDate fechaVencimiento) {
    }
}
