package ar.org.cmm.ordenes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class OrdenDtos {

    private OrdenDtos() {
    }

    /**
     * Pedido de emisión de una orden.
     *
     * @param enviarAAuditoria el médico pide que la orden pase por auditoría aunque no lo exija ninguna regla (M2)
     * @param motivoAuditoria  obligatorio cuando {@code enviarAAuditoria} es verdadero
     */
    public record EmitirOrdenRequest(
            @NotNull Long afiliadoId,
            @NotNull Long prestadorId,
            @NotEmpty List<@Valid ItemRequest> items,
            String medicoSolicitante,
            String observaciones,
            Boolean enviarAAuditoria,
            String motivoAuditoria
    ) {
        public EmitirOrdenRequest(Long afiliadoId, Long prestadorId, List<ItemRequest> items) {
            this(afiliadoId, prestadorId, items, null, null, null, null);
        }
    }

    public record ItemRequest(
            @NotNull Long practicaId,
            @NotNull @Positive Integer cantidad
    ) {
    }

    public record OrdenResponse(
            Long id,
            String numero,
            Long afiliadoId,
            Long prestadorId,
            LocalDate fechaEmision,
            String estado,
            BigDecimal totalValorConvenido,
            BigDecimal totalCargoEntidad,
            BigDecimal totalCoseguro
    ) {
    }

    /** Cálculo previo a emitir: lo que pagaría el afiliado y si la orden iría a auditoría. No guarda nada. */
    public record SimulacionResponse(
            List<ItemCalculado> items,
            BigDecimal totalValorConvenido,
            BigDecimal totalCargoEntidad,
            BigDecimal totalCoseguro,
            boolean vaAAuditoria,
            String motivoAuditoria,
            int ordenesEnElMes,
            int topeMensual
    ) {
    }

    public record ItemCalculado(
            Long practicaId,
            String codigo,
            String descripcion,
            int cantidad,
            BigDecimal valorUnitario,
            BigDecimal porcentajeCobertura,
            BigDecimal cargoEntidad,
            BigDecimal coseguro,
            boolean requiereAuditoria
    ) {
    }

    /** Fila de los listados de órdenes (Mesa de Entradas, buzón del auditor, ficha del afiliado). */
    public record OrdenResumen(
            Long id,
            String numero,
            LocalDate fechaEmision,
            String estado,
            Long afiliadoId,
            String nroAfiliado,
            String afiliado,
            String prestador,
            BigDecimal totalCoseguro,
            LocalDate fechaVencimiento,
            boolean vencida,
            String motivoAuditoria,
            LocalDateTime fechaResolucion
    ) {
    }

    public record OrdenDetalle(
            OrdenResumen orden,
            String planCodigo,
            String medicoSolicitante,
            String observaciones,
            String emitidaPor,
            BigDecimal totalValorConvenido,
            BigDecimal totalCargoEntidad,
            List<ItemDetalle> items,
            List<HistorialEstado> historial,
            List<DecisionAuditoria> auditorias
    ) {
    }

    public record ItemDetalle(
            Long id,
            Long practicaId,
            String codigo,
            String descripcion,
            int cantidadSolicitada,
            int cantidad,
            BigDecimal valorUnitario,
            BigDecimal porcentajeCobertura,
            BigDecimal cargoEntidad,
            BigDecimal coseguro,
            boolean requiereAuditoria
    ) {
    }

    public record HistorialEstado(
            Long id,
            String estadoAnterior,
            String estadoNuevo,
            LocalDateTime fecha,
            String usuario,
            String comentario
    ) {
    }

    public record DecisionAuditoria(
            String decision,
            String auditor,
            String comentario,
            LocalDateTime fecha
    ) {
    }

    /**
     * Decisión del médico auditor.
     *
     * @param decision   AUTORIZADA o RECHAZADA
     * @param comentario motivo; obligatorio al rechazar o al autorizar con cambios, porque se informa al afiliado (M9)
     * @param items      cantidades autorizadas por práctica (0 = no se autoriza); vacío = se autoriza lo pedido
     */
    public record DecisionRequest(
            @NotBlank String decision,
            String comentario,
            List<@Valid ItemDecision> items
    ) {
        public DecisionRequest(String decision, String comentario) {
            this(decision, comentario, null);
        }
    }

    public record ItemDecision(
            @NotNull Long itemId,
            @NotNull Integer cantidad
    ) {
    }

    public record AnularRequest(@NotBlank String motivo) {
    }

    /** @param aviso texto para el operador cuando el coseguro ya se cobró y hace falta una nota de crédito */
    public record AnulacionResponse(OrdenResumen orden, String aviso) {
    }

    /** Orden resuelta por auditoría, para el aviso a Mesa de Entradas. */
    public record AvisoResolucion(
            Long id,
            String numero,
            String estado,
            String afiliado,
            LocalDateTime fechaResolucion,
            String comentario
    ) {
    }
}
