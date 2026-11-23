package ar.org.cmm.cobertura;

import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.contratos.PrestadorConsulta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrestadorConsultaImpl implements PrestadorConsulta {

    private final PrestadorRepository prestadores;

    public PrestadorConsultaImpl(PrestadorRepository prestadores) {
        this.prestadores = prestadores;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrestadorResumen> todos() {
        return prestadores.findAllByOrderByNombreAsc().stream().map(PrestadorConsultaImpl::resumen).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PrestadorResumen obtener(Long prestadorId) {
        return prestadores.findById(prestadorId).map(PrestadorConsultaImpl::resumen)
                .orElseThrow(() -> new RecursoNoEncontradoException("Prestador", prestadorId));
    }

    private static PrestadorResumen resumen(Prestador p) {
        return new PrestadorResumen(p.getId(), p.getNombre(), p.getCuit(), p.getTipo(), p.isActivo());
    }
}
