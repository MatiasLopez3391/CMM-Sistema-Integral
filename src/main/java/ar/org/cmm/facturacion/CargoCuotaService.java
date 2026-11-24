package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.AfiliadoConsulta.AfiliadoResumen;
import ar.org.cmm.contratos.CargoCuotaServicio;
import ar.org.cmm.contratos.CoseguroServicio;
import ar.org.cmm.contratos.CoseguroServicio.CalculoCoseguro;
import ar.org.cmm.facturacion.FacturacionDtos.CalculoCoseguroRequest;
import ar.org.cmm.facturacion.FacturacionDtos.CargoCuotaDto;
import ar.org.cmm.facturacion.FacturacionDtos.CargoManualRequest;
import ar.org.cmm.facturacion.FacturacionDtos.HistorialCosegurosDto;
import ar.org.cmm.facturacion.FacturacionDtos.TotalPeriodoCosegurosDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Coseguros que se cobran en la cuota del mes siguiente (T42) y su historial por período (T43).
 *
 * <p>Implementa el contrato {@link CargoCuotaServicio} que usará F5 al emitir órdenes. Mientras tanto,
 * Liquidaciones los carga a mano (con importe propio o calculado según la cobertura del plan).
 */
@Service
public class CargoCuotaService implements CargoCuotaServicio {

    static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyyMM");

    private final CargoCuotaRepository repo;
    private final ComprobanteRepository comprobantes;
    private final AfiliadoConsulta afiliados;
    private final CoseguroServicio coseguros;
    private final AuditoriaService auditoria;

    public CargoCuotaService(CargoCuotaRepository repo, ComprobanteRepository comprobantes, AfiliadoConsulta afiliados,
                             CoseguroServicio coseguros, AuditoriaService auditoria) {
        this.repo = repo;
        this.comprobantes = comprobantes;
        this.afiliados = afiliados;
        this.coseguros = coseguros;
        this.auditoria = auditoria;
    }

    // ------------------------------------------------------------------ Contrato para F5

    @Override
    @Transactional
    public Long registrarCoseguro(CoseguroParaCuota c) {
        if (c.origenRef() == null || c.origenRef().isBlank()) {
            throw new ReglaNegocioException("Falta el número de la orden.");
        }
        String ref = c.origenRef().trim();
        return repo.findByOrigenAndOrigenRef(CargoCuota.ORIGEN_ORDEN, ref)
                .map(CargoCuota::getId)
                .orElseGet(() -> crear(c.afiliadoId(), c.fecha(), c.importe(), c.concepto(), null,
                        CargoCuota.ORIGEN_ORDEN, ref).getId());
    }

    @Override
    @Transactional
    public AnulacionCoseguro anularCoseguro(String origenRef, String motivo) {
        var cargo = repo.findByOrigenAndOrigenRef(CargoCuota.ORIGEN_ORDEN, origenRef == null ? "" : origenRef.trim());
        if (cargo.isEmpty()) {
            return new AnulacionCoseguro(ResultadoAnulacion.SIN_CARGO, null);
        }
        CargoCuota c = cargo.get();
        if (CargoCuota.FACTURADO.equals(c.getEstado())) {
            return new AnulacionCoseguro(ResultadoAnulacion.YA_FACTURADO, c.getPeriodoDestino());
        }
        if (CargoCuota.ANULADO.equals(c.getEstado())) {
            return new AnulacionCoseguro(ResultadoAnulacion.YA_ANULADO, c.getPeriodoDestino());
        }
        anular(c, motivo);
        return new AnulacionCoseguro(ResultadoAnulacion.ANULADO, c.getPeriodoDestino());
    }

    // ------------------------------------------------------------------ Carga manual (Liquidaciones)

    @Transactional
    public CargoCuotaDto cargarManual(CargoManualRequest req) {
        CargoCuota c = crear(req.afiliadoId(), req.fecha(), req.importe(), req.concepto(), req.periodoDestino(),
                CargoCuota.ORIGEN_MANUAL, null);
        return dtos(List.of(c)).get(0);
    }

    @Transactional
    public void anular(Long id, String motivo) {
        anular(repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Coseguro", id)), motivo);
    }

    /** Coseguro según la cobertura del plan del afiliado (regla de F2: valor convenido × (1 − cobertura)). */
    @Transactional(readOnly = true)
    public CalculoCoseguro calcular(CalculoCoseguroRequest req) {
        AfiliadoResumen a = afiliados.obtener(req.afiliadoId());
        return coseguros.calcular(req.prestadorId(), req.practicaId(), a.planId(), req.fecha() != null ? req.fecha() : LocalDate.now());
    }

    // ------------------------------------------------------------------ Consultas

    /** Historial de coseguros entre dos períodos de cuota (AAAAMM), con totales por mes. */
    @Transactional(readOnly = true)
    public HistorialCosegurosDto historial(String desde, String hasta, String estado, Long afiliadoId) {
        YearMonth d = GeneracionCuotasService.parsearPeriodo(desde);
        YearMonth h = GeneracionCuotasService.parsearPeriodo(hasta);
        if (h.isBefore(d)) {
            throw new ReglaNegocioException("El período 'hasta' es anterior a 'desde'.");
        }
        Long titularId = afiliadoId == null ? null : afiliados.obtener(afiliadoId).idTitular();
        String est = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase();
        List<CargoCuota> lista = repo.buscar(d.format(PERIODO), h.format(PERIODO), est, titularId);

        Map<String, BigDecimal[]> porPeriodo = new TreeMap<>(java.util.Comparator.reverseOrder());
        Map<String, Integer> cantidades = new HashMap<>();
        BigDecimal pendiente = BigDecimal.ZERO, facturado = BigDecimal.ZERO;
        for (CargoCuota c : lista) {
            BigDecimal[] t = porPeriodo.computeIfAbsent(c.getPeriodoDestino(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            int i = switch (c.getEstado()) {
                case CargoCuota.PENDIENTE -> 0;
                case CargoCuota.FACTURADO -> 1;
                default -> 2;
            };
            t[i] = t[i].add(c.getImporte());
            if (i < 2) {
                cantidades.merge(c.getPeriodoDestino(), 1, Integer::sum);
            }
            if (i == 0) {
                pendiente = pendiente.add(c.getImporte());
            } else if (i == 1) {
                facturado = facturado.add(c.getImporte());
            }
        }
        List<TotalPeriodoCosegurosDto> totales = new ArrayList<>();
        porPeriodo.forEach((per, t) -> totales.add(new TotalPeriodoCosegurosDto(per, cantidades.getOrDefault(per, 0), t[0], t[1], t[2])));
        return new HistorialCosegurosDto(d.format(PERIODO), h.format(PERIODO), dtos(lista), totales, pendiente, facturado);
    }

    /** Coseguros del grupo familiar (para la ficha del afiliado). */
    @Transactional(readOnly = true)
    public List<CargoCuotaDto> delGrupo(Long afiliadoId) {
        Long titularId = afiliados.obtener(afiliadoId).idTitular();
        return dtos(repo.buscar("000000", "999999", null, titularId));
    }

    // ------------------------------------------------------------------ Uso interno (generación de cuotas)

    List<CargoCuota> pendientesParaCuota(Long titularId, String periodo) {
        return repo.pendientesParaCuota(titularId, periodo);
    }

    void marcarFacturados(List<CargoCuota> cargos, Long comprobanteId) {
        for (CargoCuota c : cargos) {
            c.setEstado(CargoCuota.FACTURADO);
            c.setComprobanteId(comprobanteId);
        }
    }

    // ------------------------------------------------------------------ Privados

    private CargoCuota crear(Long afiliadoId, LocalDate fecha, BigDecimal importe, String concepto, String periodoDestino,
                             String origen, String origenRef) {
        if (afiliadoId == null) {
            throw new ReglaNegocioException("Indique el afiliado.");
        }
        if (fecha == null) {
            throw new ReglaNegocioException("Indique la fecha.");
        }
        if (importe == null || importe.signum() <= 0) {
            throw new ReglaNegocioException("El importe del coseguro debe ser mayor a cero.");
        }
        if (concepto == null || concepto.isBlank()) {
            throw new ReglaNegocioException("Indique el concepto (práctica y prestador).");
        }
        AfiliadoResumen a = afiliados.obtener(afiliadoId);
        if (!"ACTIVO".equals(a.estado())) {
            throw new ReglaNegocioException("El afiliado " + a.nroAfiliado() + " está dado de baja.");
        }
        YearMonth destino = periodoDestino == null || periodoDestino.isBlank()
                ? YearMonth.from(fecha).plusMonths(1)
                : GeneracionCuotasService.parsearPeriodo(periodoDestino);
        if (destino.isBefore(YearMonth.from(fecha))) {
            throw new ReglaNegocioException("El coseguro no puede ir a una cuota anterior a la fecha de la prestación.");
        }
        CargoCuota c = new CargoCuota();
        c.setTitularId(a.idTitular());
        c.setAfiliadoId(a.id());
        c.setTipo(CargoCuota.TIPO_COSEGURO);
        c.setConcepto(concepto.trim().length() > 200 ? concepto.trim().substring(0, 200) : concepto.trim());
        c.setFecha(fecha);
        c.setImporte(importe.setScale(2, RoundingMode.HALF_UP));
        c.setPeriodoDestino(destino.format(PERIODO));
        c.setOrigen(origen);
        c.setOrigenRef(origenRef);
        c.setEstado(CargoCuota.PENDIENTE);
        CargoCuota guardado = repo.save(c);
        auditoria.registrar("cargo_cuota", guardado.getId(), "ALTA",
                origen + (origenRef != null ? " " + origenRef : "") + " · " + a.nroAfiliado() + " · $" + c.getImporte()
                        + " a la cuota " + c.getPeriodoDestino());
        return guardado;
    }

    private void anular(CargoCuota c, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("Indique el motivo de la anulación.");
        }
        if (CargoCuota.FACTURADO.equals(c.getEstado())) {
            throw new ReglaNegocioException("El coseguro ya se facturó en una cuota: corríjalo con una nota de crédito.");
        }
        if (CargoCuota.ANULADO.equals(c.getEstado())) {
            return;
        }
        c.setEstado(CargoCuota.ANULADO);
        c.setMotivoAnulacion(motivo.trim().length() > 200 ? motivo.trim().substring(0, 200) : motivo.trim());
        auditoria.registrar("cargo_cuota", c.getId(), "ANULACION", c.getMotivoAnulacion());
    }

    private List<CargoCuotaDto> dtos(List<CargoCuota> lista) {
        Map<Long, AfiliadoResumen> cache = new HashMap<>();
        Map<Long, String> numeros = new HashMap<>();
        List<Long> compIds = lista.stream().map(CargoCuota::getComprobanteId).filter(java.util.Objects::nonNull).distinct().toList();
        comprobantes.findAllById(compIds).forEach(c -> numeros.put(c.getId(), c.getNumero()));
        List<CargoCuotaDto> res = new ArrayList<>();
        for (CargoCuota c : lista) {
            AfiliadoResumen t = cache.computeIfAbsent(c.getTitularId(), afiliados::obtener);
            AfiliadoResumen a = cache.computeIfAbsent(c.getAfiliadoId(), afiliados::obtener);
            res.add(new CargoCuotaDto(c.getId(), t.id(), t.nroAfiliado(), t.apellidoNombre(), a.id(), a.nroAfiliado(),
                    a.apellidoNombre(), c.getTipo(), c.getConcepto(), c.getFecha(), c.getImporte(), c.getPeriodoDestino(),
                    c.getOrigen(), c.getOrigenRef(), c.getEstado(), c.getComprobanteId(), numeros.get(c.getComprobanteId()),
                    c.getCreatedBy(), c.getMotivoAnulacion()));
        }
        return res;
    }
}
