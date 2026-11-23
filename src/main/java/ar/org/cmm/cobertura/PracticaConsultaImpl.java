package ar.org.cmm.cobertura;

import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.contratos.PracticaConsulta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PracticaConsultaImpl implements PracticaConsulta {

    private final PracticaRepository practicas;

    public PracticaConsultaImpl(PracticaRepository practicas) {
        this.practicas = practicas;
    }

    @Override
    @Transactional(readOnly = true)
    public PracticaResumen obtener(Long practicaId) {
        return practicas.findById(practicaId).map(PracticaConsultaImpl::resumen)
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica", practicaId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PracticaResumen> porCodigoCmm(String codigoCmm) {
        return practicas.findByCodigoCmm(codigoCmm.trim()).map(PracticaConsultaImpl::resumen);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticaResumen> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return practicas.findAllByOrderByCodigoCmmAsc().stream().map(PracticaConsultaImpl::resumen).toList();
        }
        return practicas.buscar(texto.trim()).stream().map(PracticaConsultaImpl::resumen).toList();
    }

    private static PracticaResumen resumen(Practica p) {
        return new PracticaResumen(p.getId(), p.getCodigoCmm(), p.getCodigoNomenclador(), p.getDescripcion(), p.getRubro(),
                p.isRequiereAuditoria());
    }
}
