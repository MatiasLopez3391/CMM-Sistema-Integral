package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.CoberturaRubroDto;
import ar.org.cmm.cobertura.CoberturaDtos.DerivadoraDto;
import ar.org.cmm.cobertura.CoberturaDtos.DerivadoraRequest;
import ar.org.cmm.cobertura.CoberturaDtos.PlanDto;
import ar.org.cmm.cobertura.CoberturaDtos.PlanRequest;
import ar.org.cmm.cobertura.CoberturaDtos.PracticaDto;
import ar.org.cmm.cobertura.CoberturaDtos.PracticaRequest;
import ar.org.cmm.cobertura.CoberturaDtos.PrestadorDto;
import ar.org.cmm.cobertura.CoberturaDtos.PrestadorRequest;
import ar.org.cmm.cobertura.CoberturaDtos.ValorRequest;
import ar.org.cmm.cobertura.CoberturaDtos.ValorVigenteDto;
import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CoberturaService {

    private final PlanRepository planes;
    private final DerivadoraRepository derivadoras;
    private final PrestadorRepository prestadores;
    private final PracticaRepository practicas;
    private final ValorConvenidoRepository valores;
    private final AuditoriaService auditoria;
    private final JdbcTemplate jdbc;

    public CoberturaService(PlanRepository planes, DerivadoraRepository derivadoras, PrestadorRepository prestadores,
                            PracticaRepository practicas, ValorConvenidoRepository valores, AuditoriaService auditoria,
                            JdbcTemplate jdbc) {
        this.planes = planes;
        this.derivadoras = derivadoras;
        this.prestadores = prestadores;
        this.practicas = practicas;
        this.valores = valores;
        this.auditoria = auditoria;
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ Planes

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

        // Excepciones por rubro: se reemplazan por las recibidas.
        Map<String, BigDecimal> nuevas = new LinkedHashMap<>();
        if (req.coberturasPorRubro() != null) {
            for (CoberturaRubroDto c : req.coberturasPorRubro()) {
                nuevas.put(c.rubro().trim().toUpperCase(), c.porcentaje());
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
        Plan guardado = planes.save(plan);
        auditoria.registrar("plan", guardado.getId(), id == null ? "ALTA" : "MODIFICACION",
                guardado.getCodigo() + " cobertura " + guardado.getCoberturaPorcentaje() + "%");
        return PlanDto.de(guardado);
    }

    // ------------------------------------------------------------------ Derivadoras

    @Transactional(readOnly = true)
    public List<DerivadoraDto> listarDerivadoras() {
        Map<Long, Long> conteo = new java.util.HashMap<>();
        jdbc.query("select derivadora_id, count(*) c from afiliado where derivadora_id is not null and estado = 'ACTIVO' group by derivadora_id",
                rs -> {
                    conteo.put(rs.getLong(1), rs.getLong(2));
                });
        return derivadoras.findAllByOrderByNombreAsc().stream()
                .map(d -> new DerivadoraDto(d.getId(), d.getNombre(), d.getCuit(), d.getConvenio(), d.getVigenteDesde(),
                        d.getVigenteHasta(), d.isActiva(), conteo.getOrDefault(d.getId(), 0L)))
                .toList();
    }

    @Transactional
    public Long guardarDerivadora(Long id, DerivadoraRequest req) {
        Derivadora d = id == null ? new Derivadora() : derivadoras.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Derivadora", id));
        if (req.vigenteDesde() != null && req.vigenteHasta() != null && req.vigenteHasta().isBefore(req.vigenteDesde())) {
            throw new ReglaNegocioException("La vigencia 'hasta' no puede ser anterior a 'desde'.");
        }
        d.setNombre(req.nombre().trim());
        d.setCuit(normalizarCuit(req.cuit()));
        d.setConvenio(req.convenio());
        d.setVigenteDesde(req.vigenteDesde());
        d.setVigenteHasta(req.vigenteHasta());
        d.setActiva(id == null || req.activa());
        Long nuevoId = derivadoras.save(d).getId();
        auditoria.registrar("derivadora", nuevoId, id == null ? "ALTA" : "MODIFICACION", d.getNombre());
        return nuevoId;
    }

    // ------------------------------------------------------------------ Prestadores y prácticas

    @Transactional(readOnly = true)
    public List<PrestadorDto> listarPrestadores() {
        return prestadores.findAllByOrderByNombreAsc().stream().map(PrestadorDto::de).toList();
    }

    @Transactional
    public PrestadorDto guardarPrestador(Long id, PrestadorRequest req) {
        Prestador p = id == null ? new Prestador() : prestadores.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Prestador", id));
        p.setNombre(req.nombre().trim());
        p.setCuit(normalizarCuit(req.cuit()));
        p.setTipo(req.tipo().trim().toUpperCase());
        p.setEmail(req.email() == null || req.email().isBlank() ? null : req.email().trim());
        p.setActivo(id == null || req.activo());
        Prestador guardado = prestadores.save(p);
        auditoria.registrar("prestador", guardado.getId(), id == null ? "ALTA" : "MODIFICACION", guardado.getNombre());
        return PrestadorDto.de(guardado);
    }

    @Transactional(readOnly = true)
    public List<PracticaDto> listarPracticas() {
        return practicas.findAllByOrderByCodigoCmmAsc().stream().map(PracticaDto::de).toList();
    }

    @Transactional
    public PracticaDto crearPractica(PracticaRequest req) {
        practicas.findByCodigoCmm(req.codigoCmm().trim()).ifPresent(p -> {
            throw new ReglaNegocioException("Ya existe la práctica " + req.codigoCmm());
        });
        Practica p = new Practica();
        p.setCodigoCmm(req.codigoCmm().trim());
        p.setCodigoNomenclador(req.codigoNomenclador());
        p.setDescripcion(req.descripcion().trim());
        p.setRubro(req.rubro().trim().toUpperCase());
        p.setRequiereAuditoria(req.requiereAuditoria());
        return PracticaDto.de(practicas.save(p));
    }

    // ------------------------------------------------------------------ Valores convenidos

    /**
     * Valores vigentes a una fecha para un prestador, con la vigencia anterior para mostrar el aumento.
     */
    @Transactional(readOnly = true)
    public List<ValorVigenteDto> valoresVigentes(Long prestadorId, LocalDate fecha) {
        prestadores.findById(prestadorId).orElseThrow(() -> new RecursoNoEncontradoException("Prestador", prestadorId));
        List<ValorConvenido> historial = valores.historial(prestadorId); // por práctica, vigencia desc
        List<ValorVigenteDto> resultado = new ArrayList<>();
        Set<Long> vistas = new HashSet<>();
        for (int i = 0; i < historial.size(); i++) {
            ValorConvenido v = historial.get(i);
            Long practicaId = v.getPractica().getId();
            if (vistas.contains(practicaId) || v.getVigenteDesde().isAfter(fecha)) {
                continue;
            }
            vistas.add(practicaId);
            ValorConvenido anterior = (i + 1 < historial.size() && historial.get(i + 1).getPractica().getId().equals(practicaId))
                    ? historial.get(i + 1) : null;
            BigDecimal variacion = null;
            if (anterior != null && anterior.getImporte().signum() > 0) {
                variacion = v.getImporte().subtract(anterior.getImporte())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(anterior.getImporte(), 2, RoundingMode.HALF_UP);
            }
            Practica pr = v.getPractica();
            resultado.add(new ValorVigenteDto(practicaId, pr.getCodigoCmm(), pr.getCodigoNomenclador(), pr.getDescripcion(),
                    pr.getRubro(), v.getVigenteDesde(), v.getImporte(),
                    anterior != null ? anterior.getVigenteDesde() : null,
                    anterior != null ? anterior.getImporte() : null, variacion));
        }
        return resultado;
    }

    @Transactional
    public void guardarValor(Long prestadorId, ValorRequest req, String origen) {
        Prestador prestador = prestadores.findById(prestadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Prestador", prestadorId));
        Practica practica = practicas.findById(req.practicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica", req.practicaId()));
        ValorConvenido v = valores.findByPrestadorIdAndPracticaIdAndVigenteDesde(prestadorId, practica.getId(), req.vigenteDesde())
                .orElseGet(ValorConvenido::new);
        v.setPrestador(prestador);
        v.setPractica(practica);
        v.setVigenteDesde(req.vigenteDesde());
        v.setImporte(req.importe().setScale(2, RoundingMode.HALF_UP));
        v.setOrigen(origen);
        valores.save(v);
    }

    static String normalizarCuit(String cuit) {
        if (cuit == null || cuit.isBlank()) {
            return null;
        }
        String digitos = cuit.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            throw new ReglaNegocioException("El CUIT debe tener 11 dígitos.");
        }
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }
}
