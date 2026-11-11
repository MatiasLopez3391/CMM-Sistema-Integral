package ar.org.cmm.afiliados;

import ar.org.cmm.afiliados.Afiliado.EstadoAfiliado;
import ar.org.cmm.afiliados.Afiliado.Parentesco;
import ar.org.cmm.afiliados.AfiliadoDtos.AfiliadoFicha;
import ar.org.cmm.afiliados.AfiliadoDtos.AfiliadoFila;
import ar.org.cmm.afiliados.AfiliadoDtos.AfiliadoRequest;
import ar.org.cmm.afiliados.AfiliadoDtos.AreaDto;
import ar.org.cmm.afiliados.AfiliadoDtos.BajaRequest;
import ar.org.cmm.afiliados.AfiliadoDtos.IntegranteRequest;
import ar.org.cmm.afiliados.AfiliadoDtos.PaginaDto;
import ar.org.cmm.afiliados.AfiliadoDtos.SectorDto;
import ar.org.cmm.cobertura.DerivadoraRepository;
import ar.org.cmm.cobertura.PlanRepository;
import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.NumeradorService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AfiliadoService {

    static final String NUMERADOR_BASE = "AFILIADO_BASE";

    private final AfiliadoRepository afiliados;
    private final AreaRepository areas;
    private final SectorRepository sectores;
    private final PlanRepository planes;
    private final DerivadoraRepository derivadoras;
    private final BloqueoHistorialRepository bloqueos;
    private final NumeradorService numerador;
    private final AuditoriaService auditoria;

    public AfiliadoService(AfiliadoRepository afiliados, AreaRepository areas, SectorRepository sectores, PlanRepository planes,
                           DerivadoraRepository derivadoras, BloqueoHistorialRepository bloqueos, NumeradorService numerador,
                           AuditoriaService auditoria) {
        this.afiliados = afiliados;
        this.areas = areas;
        this.sectores = sectores;
        this.planes = planes;
        this.derivadoras = derivadoras;
        this.bloqueos = bloqueos;
        this.numerador = numerador;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public PaginaDto<AfiliadoFila> buscar(String texto, EstadoAfiliado estado, Long planId, boolean soloTitulares,
                                          Boolean bloqueado, int pagina, int tamanio) {
        String t = texto == null || texto.isBlank() ? null : texto.trim();
        Page<Afiliado> page = afiliados.buscar(t, estado, planId, soloTitulares, bloqueado,
                PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanio, 1), 200)));
        return new PaginaDto<>(page.getContent().stream().map(AfiliadoFila::de).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AfiliadoFicha ficha(Long id) {
        Afiliado a = afiliados.findWithDetalleById(id).orElseThrow(() -> new RecursoNoEncontradoException("Afiliado", id));
        Afiliado titular = a.titularDelGrupo();
        List<AfiliadoFila> grupo = afiliados.grupoFamiliar(titular.getId()).stream().map(AfiliadoFila::de).toList();
        String ultimaMarca = bloqueos.findByAfiliadoIdOrderByFechaDesc(titular.getId()).stream()
                .filter(b -> "BLOQUEO".equals(b.getAccion()))
                .map(BloqueoHistorial::getMarca).findFirst().orElse(null);
        return new AfiliadoFicha(
                a.getId(), a.getNroAfiliado(), a.getTitular() != null ? a.getTitular().getId() : null,
                a.getTitular() != null ? a.getTitular().apellidoNombre() : null, a.getParentesco().name(),
                a.getApellido(), a.getNombre(), a.getTipoDocumento(), a.getDocumento(), a.getCuil(),
                a.getFechaNacimiento(), a.edad(), a.getSexo(), a.getEmail(), a.getTelefono(), a.getDomicilio(), a.getLocalidad(),
                a.getPlan().getId(), a.getPlan().getCodigo(), a.getPlan().getNombre(), a.getPlan().getCoberturaPorcentaje(),
                a.getArea().getId(), a.getArea().getNombre(), a.getSector().getId(), a.getSector().getNombre(),
                a.getDerivadora() != null ? a.getDerivadora().getId() : null,
                a.getDerivadora() != null ? a.getDerivadora().getNombre() : null,
                a.getViaCobro(), a.getCarencia().name(),
                a.getEstadoEspecial(), a.getEstadoEspecialDesde(), a.getEstadoEspecialHasta(), a.estadoEspecialVigente(LocalDate.now()),
                a.getEstado().name(), a.getFechaAlta(), a.getFechaBaja(), a.getMotivoBaja(),
                titular.isBloqueado(), titular.isBloqueado() ? ultimaMarca : null,
                a.getCreatedAt(), a.getCreatedBy(), a.getUpdatedAt(), a.getUpdatedBy(),
                grupo);
    }

    @Transactional
    public Long crearTitular(AfiliadoRequest req) {
        validarDocumentoLibre(req.tipoDocumento(), req.documento(), null);
        validarVigencia(req.estadoEspecialDesde(), req.estadoEspecialHasta());
        Afiliado a = new Afiliado();
        long base = numerador.siguiente(NUMERADOR_BASE);
        a.setNroAfiliado(String.format("%010d00", base));
        a.setParentesco(Parentesco.TITULAR);
        aplicarDatos(a, req);
        a.setEstado(EstadoAfiliado.ACTIVO);
        a.setFechaAlta(req.fechaAlta() != null ? req.fechaAlta() : LocalDate.now());
        Afiliado guardado = afiliados.save(a);
        auditoria.registrar("afiliado", guardado.getId(), "ALTA", "Titular " + guardado.getNroAfiliado() + " " + guardado.apellidoNombre());
        return guardado.getId();
    }

    @Transactional
    public void modificar(Long id, AfiliadoRequest req) {
        Afiliado a = afiliados.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Afiliado", id));
        if (a.getEstado() == EstadoAfiliado.BAJA) {
            throw new ReglaNegocioException("El afiliado está dado de baja; no se puede modificar.");
        }
        validarDocumentoLibre(req.tipoDocumento(), req.documento(), id);
        validarVigencia(req.estadoEspecialDesde(), req.estadoEspecialHasta());
        String planAnterior = a.getPlan().getCodigo();
        aplicarDatos(a, req);
        if (a.esTitular()) {
            // El plan, área, sector y vía de cobro son del grupo: se propagan a los integrantes.
            for (Afiliado integrante : afiliados.grupoFamiliar(a.getId())) {
                if (!integrante.getId().equals(a.getId())) {
                    integrante.setPlan(a.getPlan());
                    integrante.setArea(a.getArea());
                    integrante.setSector(a.getSector());
                    integrante.setViaCobro(a.getViaCobro());
                }
            }
        }
        String detalle = planAnterior.equals(a.getPlan().getCodigo()) ? "Datos actualizados"
                : "Cambio de plan " + planAnterior + " -> " + a.getPlan().getCodigo();
        auditoria.registrar("afiliado", id, "MODIFICACION", detalle);
    }

    @Transactional
    public Long agregarIntegrante(Long titularId, IntegranteRequest req) {
        Afiliado titular = afiliados.findById(titularId).orElseThrow(() -> new RecursoNoEncontradoException("Afiliado", titularId));
        if (!titular.esTitular()) {
            throw new ReglaNegocioException("Los integrantes se agregan desde la ficha del titular.");
        }
        if (titular.getEstado() == EstadoAfiliado.BAJA) {
            throw new ReglaNegocioException("El titular está dado de baja.");
        }
        if (req.parentesco() == Parentesco.TITULAR) {
            throw new ReglaNegocioException("El parentesco de un integrante no puede ser TITULAR.");
        }
        validarDocumentoLibre(req.tipoDocumento(), req.documento(), null);
        validarVigencia(req.estadoEspecialDesde(), req.estadoEspecialHasta());

        String base = titular.getNroAfiliado().substring(0, 10);
        String ultimo = afiliados.ultimoNroDelGrupo(base);
        int sufijo = ultimo == null ? 1 : Integer.parseInt(ultimo.substring(10)) + 1;
        if (sufijo > 99) {
            throw new ReglaNegocioException("El grupo familiar alcanzó el máximo de integrantes.");
        }
        Afiliado a = new Afiliado();
        a.setNroAfiliado(base + String.format("%02d", sufijo));
        a.setTitular(titular);
        a.setParentesco(req.parentesco());
        a.setApellido(req.apellido().trim().toUpperCase());
        a.setNombre(req.nombre().trim());
        a.setTipoDocumento(req.tipoDocumento());
        a.setDocumento(req.documento().trim());
        a.setFechaNacimiento(req.fechaNacimiento());
        a.setSexo(req.sexo());
        a.setEmail(vacioANull(req.email()));
        a.setEstadoEspecial(vacioANull(req.estadoEspecial()));
        a.setEstadoEspecialDesde(req.estadoEspecialDesde());
        a.setEstadoEspecialHasta(req.estadoEspecialHasta());
        a.setPlan(titular.getPlan());
        a.setArea(titular.getArea());
        a.setSector(titular.getSector());
        a.setViaCobro(titular.getViaCobro());
        a.setCarencia(titular.getCarencia());
        a.setDomicilio(titular.getDomicilio());
        a.setLocalidad(titular.getLocalidad());
        a.setEstado(EstadoAfiliado.ACTIVO);
        a.setFechaAlta(LocalDate.now());
        Afiliado guardado = afiliados.save(a);
        auditoria.registrar("afiliado", titularId, "ALTA_INTEGRANTE", guardado.getNroAfiliado() + " " + guardado.apellidoNombre());
        return guardado.getId();
    }

    /** Baja del afiliado. La baja del titular da de baja a todo el grupo. */
    @Transactional
    public int darDeBaja(Long id, BajaRequest req) {
        Afiliado a = afiliados.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Afiliado", id));
        if (a.getEstado() == EstadoAfiliado.BAJA) {
            throw new ReglaNegocioException("El afiliado ya está dado de baja.");
        }
        List<Afiliado> afectados = a.esTitular() ? afiliados.grupoFamiliar(a.getId()) : List.of(a);
        int cantidad = 0;
        for (Afiliado x : afectados) {
            if (x.getEstado() == EstadoAfiliado.ACTIVO) {
                x.setEstado(EstadoAfiliado.BAJA);
                x.setFechaBaja(req.fechaBaja());
                x.setMotivoBaja(req.motivo());
                cantidad++;
            }
        }
        auditoria.registrar("afiliado", id, "BAJA", req.motivo() + " (" + cantidad + " integrante/s)");
        return cantidad;
    }

    @Transactional(readOnly = true)
    public List<AreaDto> areas() {
        return areas.findAllByOrderByNombreAsc().stream()
                .map(ar -> new AreaDto(ar.getId(), ar.getCodigo(), ar.getNombre(),
                        ar.getSectores().stream().map(s -> new SectorDto(s.getId(), s.getCodigo(), s.getNombre())).toList()))
                .toList();
    }

    // ------------------------------------------------------------------ auxiliares

    private void aplicarDatos(Afiliado a, AfiliadoRequest req) {
        a.setApellido(req.apellido().trim().toUpperCase());
        a.setNombre(req.nombre().trim());
        a.setTipoDocumento(req.tipoDocumento());
        a.setDocumento(req.documento().trim());
        a.setCuil(vacioANull(req.cuil()));
        a.setFechaNacimiento(req.fechaNacimiento());
        a.setSexo(req.sexo());
        a.setEmail(vacioANull(req.email()));
        a.setTelefono(vacioANull(req.telefono()));
        a.setDomicilio(vacioANull(req.domicilio()));
        a.setLocalidad(vacioANull(req.localidad()));
        a.setPlan(planes.findById(req.planId()).orElseThrow(() -> new RecursoNoEncontradoException("Plan", req.planId())));
        if (!a.getPlan().isActivo()) {
            throw new ReglaNegocioException("El plan " + a.getPlan().getCodigo() + " no está activo.");
        }
        Sector sector = sectores.findById(req.sectorId()).orElseThrow(() -> new RecursoNoEncontradoException("Sector", req.sectorId()));
        if (!sector.getArea().getId().equals(req.areaId())) {
            throw new ReglaNegocioException("El sector no pertenece al área seleccionada.");
        }
        a.setArea(sector.getArea());
        a.setSector(sector);
        a.setDerivadora(req.derivadoraId() == null ? null : derivadoras.findById(req.derivadoraId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Derivadora", req.derivadoraId())));
        a.setViaCobro(vacioANull(req.viaCobro()));
        a.setCarencia(req.carencia());
        a.setEstadoEspecial(vacioANull(req.estadoEspecial()));
        a.setEstadoEspecialDesde(req.estadoEspecialDesde());
        a.setEstadoEspecialHasta(req.estadoEspecialHasta());
    }

    private void validarDocumentoLibre(String tipo, String documento, Long idActual) {
        afiliados.findByTipoDocumentoAndDocumento(tipo, documento.trim())
                .filter(otro -> !otro.getId().equals(idActual))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("Ya existe el afiliado " + otro.getNroAfiliado() + " con " + tipo + " " + documento);
                });
    }

    private static void validarVigencia(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La vigencia del estado especial es inválida (hasta < desde).");
        }
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
