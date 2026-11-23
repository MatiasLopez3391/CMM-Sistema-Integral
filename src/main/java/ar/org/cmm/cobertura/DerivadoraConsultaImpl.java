package ar.org.cmm.cobertura;

import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.contratos.DerivadoraConsulta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DerivadoraConsultaImpl implements DerivadoraConsulta {

    private final DerivadoraRepository derivadoras;

    public DerivadoraConsultaImpl(DerivadoraRepository derivadoras) {
        this.derivadoras = derivadoras;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DerivadoraResumen> todas() {
        return derivadoras.findAllByOrderByNombreAsc().stream().map(DerivadoraConsultaImpl::resumen).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DerivadoraResumen obtener(Long derivadoraId) {
        return derivadoras.findById(derivadoraId).map(DerivadoraConsultaImpl::resumen)
                .orElseThrow(() -> new RecursoNoEncontradoException("Derivadora", derivadoraId));
    }

    private static DerivadoraResumen resumen(Derivadora d) {
        return new DerivadoraResumen(d.getId(), d.getNombre(), d.getCuit(), d.getConvenio(), d.getVigenteDesde(),
                d.getVigenteHasta(), d.isActiva());
    }
}
