package ar.org.cmm.cobertura;

import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.CoseguroServicio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Coseguro variable según la cobertura del plan (H12), en reemplazo del monto fijo
 * "a cargo del afiliado" que hoy figura en las planillas de cada prestador.
 */
@Service
public class CoseguroServicioImpl implements CoseguroServicio {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final PlanRepository planes;
    private final PracticaRepository practicas;
    private final ValorConvenidoRepository valores;

    public CoseguroServicioImpl(PlanRepository planes, PracticaRepository practicas, ValorConvenidoRepository valores) {
        this.planes = planes;
        this.practicas = practicas;
        this.valores = valores;
    }

    @Override
    @Transactional(readOnly = true)
    public CalculoCoseguro calcular(Long prestadorId, Long practicaId, Long planId, LocalDate fecha) {
        Plan plan = planes.findById(planId).orElseThrow(() -> new RecursoNoEncontradoException("Plan", planId));
        Practica practica = practicas.findById(practicaId).orElseThrow(() -> new RecursoNoEncontradoException("Práctica", practicaId));
        ValorConvenido valor = valores.vigente(prestadorId, practicaId, fecha)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El prestador no tiene valor convenido vigente al " + fecha + " para la práctica " + practica.getCodigoCmm()));
        return calcular(plan, practica, valor);
    }

    /** Tabla de coseguro de una práctica para todos los planes activos (simulador). */
    @Transactional(readOnly = true)
    public List<CalculoCoseguro> simularTodosLosPlanes(Long prestadorId, Long practicaId, LocalDate fecha) {
        Practica practica = practicas.findById(practicaId).orElseThrow(() -> new RecursoNoEncontradoException("Práctica", practicaId));
        ValorConvenido valor = valores.vigente(prestadorId, practicaId, fecha)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El prestador no tiene valor convenido vigente al " + fecha + " para la práctica " + practica.getCodigoCmm()));
        return planes.findByActivoTrueOrderByCodigoAsc().stream().map(p -> calcular(p, practica, valor)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal porcentajeCobertura(Long planId, String rubro) {
        Plan plan = planes.findById(planId).orElseThrow(() -> new RecursoNoEncontradoException("Plan", planId));
        return cobertura(plan, rubro);
    }

    private CalculoCoseguro calcular(Plan plan, Practica practica, ValorConvenido valor) {
        BigDecimal cobertura = cobertura(plan, practica.getRubro());
        BigDecimal importe = valor.getImporte();
        BigDecimal aCargoEntidad = importe.multiply(cobertura).divide(CIEN, 2, RoundingMode.HALF_UP);
        BigDecimal coseguro = importe.subtract(aCargoEntidad);
        return new CalculoCoseguro(valor.getPrestador().getId(), practica.getId(), plan.getId(), plan.getCodigo(), plan.getNombre(),
                practica.getRubro(), valor.getVigenteDesde(), importe, cobertura, aCargoEntidad, coseguro);
    }

    private BigDecimal cobertura(Plan plan, String rubro) {
        return plan.getCoberturasPorRubro().stream()
                .filter(c -> c.getRubro().equalsIgnoreCase(rubro))
                .map(CoberturaRubro::getPorcentaje)
                .findFirst()
                .orElse(plan.getCoberturaPorcentaje());
    }
}
