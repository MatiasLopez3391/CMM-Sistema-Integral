package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.PlanConsulta;
import ar.org.cmm.contratos.PlanConsulta.PlanResumen;
import ar.org.cmm.facturacion.FacturacionDtos.ActualizacionCuotaRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoActualizacion;
import ar.org.cmm.facturacion.FacturacionDtos.ValorCuotaPlanDto;
import ar.org.cmm.facturacion.FacturacionDtos.ValorCuotaRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ValorHistoricoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Valores de cuota por plan. La actualización mensual la hace Liquidaciones desde pantalla
 * (por porcentaje o valor puntual), sin pedirla al área de sistemas.
 */
@Service
public class ValorCuotaService {

    private final ValorCuotaRepository repo;
    private final PlanConsulta planes;
    private final AuditoriaService auditoria;

    public ValorCuotaService(ValorCuotaRepository repo, PlanConsulta planes, AuditoriaService auditoria) {
        this.repo = repo;
        this.planes = planes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ValorCuotaPlanDto> listar(LocalDate fecha) {
        List<ValorCuotaPlanDto> res = new ArrayList<>();
        for (PlanResumen p : planes.todos()) {
            Optional<ValorCuota> vigente = repo.vigente(p.id(), fecha);
            List<ValorHistoricoDto> historial = repo.findByPlanIdOrderByVigenteDesdeDesc(p.id()).stream()
                    .map(v -> new ValorHistoricoDto(v.getId(), v.getVigenteDesde(), v.getImporte(), v.getMotivo(), v.getCreatedBy()))
                    .toList();
            res.add(new ValorCuotaPlanDto(p.id(), p.codigo(), p.nombre(), p.coberturaPorcentaje(),
                    vigente.map(ValorCuota::getVigenteDesde).orElse(null),
                    vigente.map(ValorCuota::getImporte).orElse(null), historial));
        }
        return res;
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> vigente(Long planId, LocalDate fecha) {
        return repo.vigente(planId, fecha).map(ValorCuota::getImporte);
    }

    /** Aplica un porcentaje sobre el valor vigente y crea una nueva vigencia por plan. */
    @Transactional
    public ResultadoActualizacion actualizarPorPorcentaje(ActualizacionCuotaRequest req) {
        if (req.porcentaje().signum() == 0) {
            throw new ReglaNegocioException("El porcentaje no puede ser 0.");
        }
        List<PlanResumen> alcance = planes.todos().stream()
                .filter(PlanResumen::activo)
                .filter(p -> req.planIds() == null || req.planIds().isEmpty() || req.planIds().contains(p.id()))
                .toList();
        if (alcance.isEmpty()) {
            throw new ReglaNegocioException("No hay planes activos seleccionados.");
        }
        BigDecimal factor = BigDecimal.ONE.add(req.porcentaje().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        List<String> detalle = new ArrayList<>();
        int cantidad = 0;
        for (PlanResumen p : alcance) {
            Optional<ValorCuota> base = repo.vigente(p.id(), req.vigenteDesde().minusDays(1));
            if (base.isEmpty()) {
                detalle.add(p.codigo() + ": sin valor anterior, se omite (cargar valor puntual)");
                continue;
            }
            if (repo.findByPlanIdAndVigenteDesde(p.id(), req.vigenteDesde()).isPresent()) {
                detalle.add(p.codigo() + ": ya tiene un valor desde " + req.vigenteDesde() + ", se omite");
                continue;
            }
            BigDecimal nuevo = base.get().getImporte().multiply(factor).setScale(2, RoundingMode.HALF_UP);
            ValorCuota v = new ValorCuota();
            v.setPlanId(p.id());
            v.setVigenteDesde(req.vigenteDesde());
            v.setImporte(nuevo);
            v.setMotivo(req.motivo() == null || req.motivo().isBlank()
                    ? "Actualización " + req.porcentaje().stripTrailingZeros().toPlainString() + "%" : req.motivo().trim());
            repo.save(v);
            detalle.add(p.codigo() + ": " + base.get().getImporte() + " -> " + nuevo);
            cantidad++;
        }
        auditoria.registrar("valor_cuota", 0L, "ACTUALIZACION",
                req.porcentaje() + "% desde " + req.vigenteDesde() + " (" + cantidad + " planes)");
        return new ResultadoActualizacion(cantidad, detalle);
    }

    @Transactional
    public void guardarValorPuntual(ValorCuotaRequest req) {
        planes.obtener(req.planId());
        ValorCuota v = repo.findByPlanIdAndVigenteDesde(req.planId(), req.vigenteDesde()).orElseGet(ValorCuota::new);
        v.setPlanId(req.planId());
        v.setVigenteDesde(req.vigenteDesde());
        v.setImporte(req.importe().setScale(2, RoundingMode.HALF_UP));
        v.setMotivo(req.motivo() == null || req.motivo().isBlank() ? "Valor puntual" : req.motivo().trim());
        repo.save(v);
        auditoria.registrar("valor_cuota", req.planId(), "VALOR_PUNTUAL", req.importe() + " desde " + req.vigenteDesde());
    }
}
