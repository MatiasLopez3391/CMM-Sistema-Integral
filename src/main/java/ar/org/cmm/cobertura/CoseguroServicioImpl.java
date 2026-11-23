package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.coseguro.CalculadoraCoseguro;
import ar.org.cmm.cobertura.coseguro.CoberturaDePlan;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.CoseguroServicio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fachada del contrato CoseguroServicio: busca los datos y delega el cálculo en la estrategia
 * que corresponda (ver {@link CalculadoraCoseguro}).
 */
@Service
public class CoseguroServicioImpl implements CoseguroServicio {

    private final PlanRepository planes;
    private final PracticaRepository practicas;
    private final ValorConvenidoRepository valores;
    private final List<CalculadoraCoseguro> calculadoras; // Spring las inyecta ordenadas por @Order
    private final CoberturaDePlan cobertura;

    public CoseguroServicioImpl(PlanRepository planes, PracticaRepository practicas, ValorConvenidoRepository valores,
                                List<CalculadoraCoseguro> calculadoras, CoberturaDePlan cobertura) {
        this.planes = planes;
        this.practicas = practicas;
        this.valores = valores;
        this.calculadoras = calculadoras;
        this.cobertura = cobertura;
    }

    @Override
    @Transactional(readOnly = true)
    public CalculoCoseguro calcular(Long prestadorId, Long practicaId, Long planId, LocalDate fecha) {
        Plan plan = planes.findById(planId).orElseThrow(() -> new RecursoNoEncontradoException("Plan", planId));
        Practica practica = practicas.findById(practicaId).orElseThrow(() -> new RecursoNoEncontradoException("Práctica", practicaId));
        return calcular(plan, practica, valorVigente(prestadorId, practica, fecha));
    }

    /** Tabla de coseguro de una práctica para todos los planes activos (simulador). */
    @Override
    @Transactional(readOnly = true)
    public List<CalculoCoseguro> simularTodosLosPlanes(Long prestadorId, Long practicaId, LocalDate fecha) {
        Practica practica = practicas.findById(practicaId).orElseThrow(() -> new RecursoNoEncontradoException("Práctica", practicaId));
        ValorConvenido valor = valorVigente(prestadorId, practica, fecha);
        return planes.findByActivoTrueOrderByCodigoAsc().stream().map(p -> calcular(p, practica, valor)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal porcentajeCobertura(Long planId, String rubro) {
        Plan plan = planes.findById(planId).orElseThrow(() -> new RecursoNoEncontradoException("Plan", planId));
        return cobertura.porcentaje(plan, rubro);
    }

    private ValorConvenido valorVigente(Long prestadorId, Practica practica, LocalDate fecha) {
        return valores.vigente(prestadorId, practica.getId(), fecha)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El prestador no tiene valor convenido vigente al " + fecha + " para la práctica " + practica.getCodigoCmm()));
    }

    private CalculoCoseguro calcular(Plan plan, Practica practica, ValorConvenido valor) {
        return calculadoras.stream()
                .filter(c -> c.aplicaA(plan, practica))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("No hay una regla de coseguro para el plan " + plan.getCodigo()))
                .calcular(plan, practica, valor);
    }
}
