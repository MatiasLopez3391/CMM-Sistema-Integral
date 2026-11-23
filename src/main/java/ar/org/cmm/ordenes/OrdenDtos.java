
package ar.org.cmm.ordenes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class OrdenDtos {

    private OrdenDtos() {
    }

    public record EmitirOrdenRequest(
            @NotNull Long afiliadoId,
            @NotNull Long prestadorId,
            @NotEmpty List<@Valid ItemRequest> items
    ) {
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
}
