package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.CoberturaRubroDto;
import ar.org.cmm.cobertura.CoberturaDtos.PlanDto;
import ar.org.cmm.cobertura.CoberturaDtos.PlanRequest;
import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** ABM de planes y de sus excepciones de cobertura por rubro. */
@Service
public class PlanService {

    private final PlanRepository planes;
    private final AuditoriaService auditoria;

    public PlanService(PlanRepository planes, AuditoriaService auditoria) {
        this.planes = planes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<PlanDto> listarPlanes() {
        return planes.findAllByOrderByCodigoAsc().stream().map(PlanDto::de).toList();
    }

    @Transactional
    public PlanDto guardarPlan(Long id, PlanRequest req) {
        Plan plan = id == null ? new Plan() : planes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Plan", id));
        planes.findByCodigo(req.codigo().trim())
                .filter(otro -> !otro.getId().equals(plan.getId()))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("Ya existe un plan con código " + req.codigo());
                });
        plan.setCodigo(req.codigo().trim());
        plan.setNombre(req.nombre().trim());
        plan.setCoberturaPorcentaje(req.coberturaPorcentaje());
        plan.setActivo(id == null || req.activo());
        reemplazarExcepciones(plan, req.coberturasPorRubro());

        Plan guardado = planes.save(plan);
        auditoria.registrar("plan", guardado.getId(), id == null ? "ALTA" : "MODIFICACION",
                guardado.getCodigo() + " cobertura " + guardado.getCoberturaPorcentaje() + "%");
        return PlanDto.de(guardado);
    }

    /** Las excepciones por rubro se reemplazan por las recibidas (se validan contra el enum Rubro). */
    private void reemplazarExcepciones(Plan plan, List<CoberturaRubroDto> recibidas) {
        Map<String, BigDecimal> nuevas = new LinkedHashMap<>();
        if (recibidas != null) {
            for (CoberturaRubroDto c : recibidas) {
                nuevas.put(Rubro.normalizar(c.rubro()), c.porcentaje());
            }
        }
        plan.getCoberturasPorRubro().removeIf(c -> !nuevas.containsKey(c.getRubro()));
        for (Map.Entry<String, BigDecimal> e : nuevas.entrySet()) {
            CoberturaRubro existente = plan.getCoberturasPorRubro().stream()
                    .filter(c -> c.getRubro().equals(e.getKey())).findFirst().orElse(null);
            if (existente == null) {
                existente = new CoberturaRubro();
                existente.setPlan(plan);
                existente.setRubro(e.getKey());
                plan.getCoberturasPorRubro().add(existente);
            }
            existente.setPorcentaje(e.getValue());
        }
    }
}
