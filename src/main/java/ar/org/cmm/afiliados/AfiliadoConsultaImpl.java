package ar.org.cmm.afiliados;

import ar.org.cmm.afiliados.Afiliado.EstadoAfiliado;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.contratos.AfiliadoConsulta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AfiliadoConsultaImpl implements AfiliadoConsulta {

    private final AfiliadoRepository afiliados;

    public AfiliadoConsultaImpl(AfiliadoRepository afiliados) {
        this.afiliados = afiliados;
    }

    @Override
    @Transactional(readOnly = true)
    public AfiliadoResumen obtener(Long afiliadoId) {
        return afiliados.findById(afiliadoId).map(AfiliadoConsultaImpl::resumen)
                .orElseThrow(() -> new RecursoNoEncontradoException("Afiliado", afiliadoId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AfiliadoResumen> porNroAfiliado(String nroAfiliado) {
        if (nroAfiliado == null) {
            return Optional.empty();
        }
        return afiliados.findByNroAfiliado(nroAfiliado.trim()).map(AfiliadoConsultaImpl::resumen);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AfiliadoResumen> titularesActivos() {
        return afiliados.titularesActivos().stream().map(AfiliadoConsultaImpl::resumen).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AfiliadoResumen> integrantesActivos(Long titularId) {
        return afiliados.grupoFamiliar(titularId).stream()
                .filter(a -> a.getEstado() == EstadoAfiliado.ACTIVO)
                .map(AfiliadoConsultaImpl::resumen).toList();
    }

    static AfiliadoResumen resumen(Afiliado a) {
        return new AfiliadoResumen(a.getId(), a.getNroAfiliado(), a.apellidoNombre(), a.getTipoDocumento() + " " + a.getDocumento(),
                a.titularDelGrupo().getEmail(), a.getTitular() != null ? a.getTitular().getId() : null,
                a.getPlan().getId(), a.getPlan().getCodigo(), a.getPlan().getNombre(), a.getEstado().name(),
                a.titularDelGrupo().isBloqueado());
    }
}
