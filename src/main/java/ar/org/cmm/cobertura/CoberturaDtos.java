package ar.org.cmm.cobertura;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTOs de la API del módulo Cobertura (la API nunca expone entidades JPA). */
public final class CoberturaDtos {

    private CoberturaDtos() {
    }

    public record PlanDto(Long id, String codigo, String nombre, BigDecimal coberturaPorcentaje, boolean activo,
                          List<CoberturaRubroDto> coberturasPorRubro) {
        static PlanDto de(Plan p) {
            return new PlanDto(p.getId(), p.getCodigo(), p.getNombre(), p.getCoberturaPorcentaje(), p.isActivo(),
                    p.getCoberturasPorRubro().stream().map(c -> new CoberturaRubroDto(c.getRubro(), c.getPorcentaje())).toList());
        }
    }

    public record CoberturaRubroDto(@NotBlank String rubro,
                                    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal porcentaje) {
    }

    public record PlanRequest(@NotBlank @Size(max = 10) String codigo,
                              @NotBlank @Size(max = 100) String nombre,
                              @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal coberturaPorcentaje,
                              boolean activo,
                              List<CoberturaRubroDto> coberturasPorRubro) {
    }

    public record DerivadoraDto(Long id, String nombre, String cuit, String convenio, LocalDate vigenteDesde,
                                LocalDate vigenteHasta, boolean activa, long afiliados) {
    }

    public record DerivadoraRequest(@NotBlank @Size(max = 120) String nombre,
                                    @Pattern(regexp = "^$|^\\d{2}-?\\d{8}-?\\d$", message = "CUIT inválido") String cuit,
                                    @Size(max = 200) String convenio,
                                    LocalDate vigenteDesde,
                                    LocalDate vigenteHasta,
                                    boolean activa) {
    }

    public record PrestadorDto(Long id, String nombre, String cuit, String tipo, String email, boolean activo) {
        static PrestadorDto de(Prestador p) {
            return new PrestadorDto(p.getId(), p.getNombre(), p.getCuit(), p.getTipo(), p.getEmail(), p.isActivo());
        }
    }

    public record PrestadorRequest(@NotBlank @Size(max = 120) String nombre,
                                   @Pattern(regexp = "^$|^\\d{2}-?\\d{8}-?\\d$", message = "CUIT inválido") String cuit,
                                   @NotBlank String tipo,
                                   String email,
                                   boolean activo) {
    }

    public record PracticaDto(Long id, String codigoCmm, String codigoNomenclador, String descripcion, String rubro,
                              boolean requiereAuditoria) {
        static PracticaDto de(Practica p) {
            return new PracticaDto(p.getId(), p.getCodigoCmm(), p.getCodigoNomenclador(), p.getDescripcion(), p.getRubro(),
                    p.isRequiereAuditoria());
        }
    }

    public record PracticaRequest(@NotBlank @Size(max = 12) String codigoCmm,
                                  @Size(max = 12) String codigoNomenclador,
                                  @NotBlank @Size(max = 200) String descripcion,
                                  @NotBlank String rubro,
                                  boolean requiereAuditoria) {
    }

    /** Valor vigente de una práctica para un prestador, con el anterior para ver la variación. */
    public record ValorVigenteDto(Long practicaId, String codigoCmm, String codigoNomenclador, String descripcion,
                                  String rubro, LocalDate vigenteDesde, BigDecimal importe,
                                  LocalDate anteriorDesde, BigDecimal importeAnterior, BigDecimal variacionPorcentaje) {
    }

    public record ValorRequest(@NotNull Long practicaId, @NotNull LocalDate vigenteDesde,
                               @NotNull @Positive BigDecimal importe) {
    }

    public record ResultadoImportacion(int filasLeidas, int valoresCreados, int valoresActualizados,
                                       int practicasCreadas, List<String> advertencias) {
    }
}
