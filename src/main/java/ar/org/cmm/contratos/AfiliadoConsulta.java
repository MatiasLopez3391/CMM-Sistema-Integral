package ar.org.cmm.contratos;

import java.util.List;
import java.util.Optional;

/**
 * Contrato publicado por F1 (Afiliados). Lo usan F2-F5 para leer afiliados sin depender
 * de las clases internas del módulo.
 */
public interface AfiliadoConsulta {

    AfiliadoResumen obtener(Long afiliadoId);

    Optional<AfiliadoResumen> porNroAfiliado(String nroAfiliado);

    /** Titulares activos (la cuota se emite al titular por todo el grupo). */
    List<AfiliadoResumen> titularesActivos();

    /** Integrantes activos del grupo, incluido el titular. */
    List<AfiliadoResumen> integrantesActivos(Long titularId);

    record AfiliadoResumen(
            Long id,
            String nroAfiliado,
            String apellidoNombre,
            String documento,
            String email,
            Long titularId,
            Long planId,
            String planCodigo,
            String planNombre,
            String estado,
            boolean bloqueado) {

        /** Id del titular del grupo (el propio id si es titular). */
        public Long idTitular() {
            return titularId != null ? titularId : id;
        }
    }
}
