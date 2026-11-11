package ar.org.cmm.cobertura;

import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.contratos.PlanConsulta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanConsultaImpl implements PlanConsulta {

    private final PlanRepository planes;

    public PlanConsultaImpl(PlanRepository planes) {
        this.planes = planes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResumen> todos() {
        return planes.findAllByOrderByCodigoAsc().stream().map(PlanConsultaImpl::resumen).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResumen obtener(Long planId) {
        return planes.findById(planId).map(PlanConsultaImpl::resumen)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan", planId));
    }

    private static PlanResumen resumen(Plan p) {
        return new PlanResumen(p.getId(), p.getCodigo(), p.getNombre(), p.getCoberturaPorcentaje(), p.isActivo());
    }
}
