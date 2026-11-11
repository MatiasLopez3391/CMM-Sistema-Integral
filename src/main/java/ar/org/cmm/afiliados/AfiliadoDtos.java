package ar.org.cmm.afiliados;

import ar.org.cmm.afiliados.Afiliado.Carencia;
import ar.org.cmm.afiliados.Afiliado.Parentesco;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AfiliadoDtos {

    private AfiliadoDtos() {
    }

    /** Fila del listado de búsqueda. */
    public record AfiliadoFila(Long id, String nroAfiliado, String apellidoNombre, String documento, String parentesco,
                               String planCodigo, String planNombre, String estado, boolean bloqueado, Long titularId) {
        static AfiliadoFila de(Afiliado a) {
            return new AfiliadoFila(a.getId(), a.getNroAfiliado(), a.apellidoNombre(), a.getTipoDocumento() + " " + a.getDocumento(),
                    a.getParentesco().name(), a.getPlan().getCodigo(), a.getPlan().getNombre(), a.getEstado().name(),
                    a.titularDelGrupo().isBloqueado(), a.getTitular() != null ? a.getTitular().getId() : null);
        }
    }

    /** Ficha completa del afiliado. */
    public record AfiliadoFicha(
            Long id, String nroAfiliado, Long titularId, String titularNombre, String parentesco,
            String apellido, String nombre, String tipoDocumento, String documento, String cuil,
            LocalDate fechaNacimiento, int edad, String sexo, String email, String telefono, String domicilio, String localidad,
            Long planId, String planCodigo, String planNombre, java.math.BigDecimal coberturaPorcentaje,
            Long areaId, String areaNombre, Long sectorId, String sectorNombre,
            Long derivadoraId, String derivadoraNombre, String viaCobro, String carencia,
            String estadoEspecial, LocalDate estadoEspecialDesde, LocalDate estadoEspecialHasta, boolean estadoEspecialVigente,
            String estado, LocalDate fechaAlta, LocalDate fechaBaja, String motivoBaja,
            boolean bloqueado, String ultimaMarcaBloqueo,
            LocalDateTime creadoEl, String creadoPor, LocalDateTime modificadoEl, String modificadoPor,
            List<AfiliadoFila> grupoFamiliar) {
    }

    /** Alta o modificación de un titular. */
    public record AfiliadoRequest(
            @NotBlank @Size(max = 80) String apellido,
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Pattern(regexp = "DNI|LC|LE|PAS|CI", message = "tipo de documento inválido") String tipoDocumento,
            @NotBlank @Pattern(regexp = "[0-9A-Za-z]{6,15}", message = "documento inválido") String documento,
            @Pattern(regexp = "^$|^\\d{2}-?\\d{8}-?\\d$", message = "CUIL inválido") String cuil,
            @NotNull @Past LocalDate fechaNacimiento,
            @NotBlank @Pattern(regexp = "[MFX]", message = "sexo debe ser M, F o X") String sexo,
            @Email String email,
            @Size(max = 40) String telefono,
            @Size(max = 150) String domicilio,
            @Size(max = 80) String localidad,
            @NotNull Long planId,
            @NotNull Long areaId,
            @NotNull Long sectorId,
            Long derivadoraId,
            String viaCobro,
            @NotNull Carencia carencia,
            @Size(max = 80) String estadoEspecial,
            LocalDate estadoEspecialDesde,
            LocalDate estadoEspecialHasta,
            LocalDate fechaAlta) {
    }

    /** Alta de un integrante del grupo familiar (hereda plan, área, sector y vía de cobro del titular). */
    public record IntegranteRequest(
            @NotBlank @Size(max = 80) String apellido,
            @NotBlank @Size(max = 80) String nombre,
            @NotNull Parentesco parentesco,
            @NotBlank @Pattern(regexp = "DNI|LC|LE|PAS|CI", message = "tipo de documento inválido") String tipoDocumento,
            @NotBlank @Pattern(regexp = "[0-9A-Za-z]{6,15}", message = "documento inválido") String documento,
            @NotNull @Past LocalDate fechaNacimiento,
            @NotBlank @Pattern(regexp = "[MFX]", message = "sexo debe ser M, F o X") String sexo,
            @Email String email,
            @Size(max = 80) String estadoEspecial,
            LocalDate estadoEspecialDesde,
            LocalDate estadoEspecialHasta) {
    }

    public record BajaRequest(@NotNull LocalDate fechaBaja, @NotBlank @Size(max = 150) String motivo) {
    }

    public record BloqueoRequest(@Size(max = 80) String marca, @NotBlank @Size(max = 500) String motivo) {
    }

    public record BloqueoDto(Long id, String accion, String marca, String motivo, String usuario, LocalDateTime fecha) {
        static BloqueoDto de(BloqueoHistorial b) {
            return new BloqueoDto(b.getId(), b.getAccion(), b.getMarca(), b.getMotivo(), b.getUsuario(), b.getFecha());
        }
    }

    public record AreaDto(Long id, String codigo, String nombre, List<SectorDto> sectores) {
    }

    public record SectorDto(Long id, String codigo, String nombre) {
    }

    public record PaginaDto<T>(List<T> contenido, int pagina, int tamanio, long total, int totalPaginas) {
    }
}
