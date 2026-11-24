package ar.org.cmm.ordenes;

import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.PracticaConsulta;
import ar.org.cmm.contratos.PrestadorConsulta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Consultas de órdenes para las pantallas de Mesa de Entradas, del auditor y de la ficha del afiliado. */
@Service
public class OrdenConsultaService {

    static final Set<String> RESUELTAS = Set.of(OrdenEstado.AUTORIZADA.name(), OrdenEstado.RECHAZADA.name());

    private final OrdenRepository ordenes;
    private final OrdenEstadoHistorialRepository historiales;
    private final AuditoriaMedicaRepository auditorias;
    private final AfiliadoConsulta afiliados;
    private final PrestadorConsulta prestadores;
    private final PracticaConsulta practicas;

    public OrdenConsultaService(OrdenRepository ordenes, OrdenEstadoHistorialRepository historiales,
                                AuditoriaMedicaRepository auditorias, AfiliadoConsulta afiliados,
                                PrestadorConsulta prestadores, PracticaConsulta practicas) {
        this.ordenes = ordenes;
        this.historiales = historiales;
        this.auditorias = auditorias;
        this.afiliados = afiliados;
        this.prestadores = prestadores;
        this.practicas = practicas;
    }

    @Transactional(readOnly = true)
    public List<OrdenDtos.OrdenResumen> buscar(Long afiliadoId, String estado, String numero, LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.of(2000, 1, 1);
        LocalDate h = hasta != null ? hasta : LocalDate.now().plusYears(1);
        String est = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase();
        String num = numero == null || numero.isBlank() ? null : numero.trim().toUpperCase();
        return resumenes(ordenes.buscar(afiliadoId, est, num, d, h));
    }

    @Transactional(readOnly = true)
    public List<OrdenDtos.OrdenResumen> pendientesDeAuditoria() {
        return resumenes(ordenes.findByEstadoOrderByFechaEmisionDesc(OrdenEstado.PENDIENTE_AUDITORIA.name()));
    }

    @Transactional(readOnly = true)
    public List<OrdenDtos.OrdenResumen> ultimasResueltas() {
        return resumenes(ordenes.findTop50ByFechaResolucionIsNotNullAndEstadoInOrderByFechaResolucionDesc(RESUELTAS));
    }

    /** Órdenes que el auditor resolvió después de {@code desde}, con el comentario de su decisión. */
    @Transactional(readOnly = true)
    public List<OrdenDtos.AvisoResolucion> resueltasDesde(LocalDateTime desde) {
        LocalDateTime d = desde != null ? desde : LocalDateTime.now().minusDays(1);
        return ordenes.findByFechaResolucionAfterAndEstadoInOrderByFechaResolucionAsc(d, RESUELTAS).stream()
                .map(o -> {
                    var ultima = auditorias.findByOrdenIdOrderByFechaDesc(o.getId()).stream().findFirst();
                    return new OrdenDtos.AvisoResolucion(o.getId(), o.getNumero(), o.getEstado(),
                            afiliados.obtener(o.getAfiliadoId()).apellidoNombre(), o.getFechaResolucion(),
                            ultima.map(AuditoriaMedica::getComentario).orElse(null));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdenDtos.OrdenResumen resumen(Long ordenId) {
        return resumenes(List.of(obtener(ordenId))).get(0);
    }

    @Transactional(readOnly = true)
    public OrdenDtos.OrdenDetalle detalle(Long ordenId) {
        Orden o = obtener(ordenId);
        var afiliado = afiliados.obtener(o.getAfiliadoId());
        var items = o.getItems().stream().map(it -> {
            var p = practicas.obtener(it.getPracticaId());
            int solicitada = it.getCantidadSolicitada() != null ? it.getCantidadSolicitada() : it.getCantidad();
            return new OrdenDtos.ItemDetalle(it.getId(), p.id(), p.codigoCmm(), p.descripcion(), solicitada,
                    it.getCantidad(), it.getValorConvenido(), it.getPorcentajeCobertura(), it.getCargoEntidad(),
                    it.getCoseguroAfiliado(), it.isRequiereAuditoria());
        }).toList();
        var historial = historiales.findByOrdenIdOrderByFechaAsc(o.getId()).stream()
                .map(h -> new OrdenDtos.HistorialEstado(h.getId(), h.getEstadoAnterior(), h.getEstadoNuevo(),
                        h.getFecha(), h.getUsuario(), h.getComentario()))
                .toList();
        var decisiones = auditorias.findByOrdenIdOrderByFechaDesc(o.getId()).stream()
                .map(a -> new OrdenDtos.DecisionAuditoria(a.getDecision(), a.getUsuarioAuditor(), a.getComentario(), a.getFecha()))
                .toList();
        return new OrdenDtos.OrdenDetalle(resumenes(List.of(o)).get(0), afiliado.planCodigo(), o.getMedicoSolicitante(),
                o.getObservaciones(), o.getEmitidaPor(), o.getTotalValorConvenido(), o.getTotalCargoEntidad(), items,
                historial, decisiones);
    }

    Orden obtener(Long ordenId) {
        return ordenes.findById(ordenId).orElseThrow(() -> new RecursoNoEncontradoException("Orden", ordenId));
    }

    /** Valor unitario de coseguro de un ítem (el total guardado dividido por la cantidad pedida). */
    static BigDecimal unitario(BigDecimal total, int cantidad) {
        return cantidad <= 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }

    private List<OrdenDtos.OrdenResumen> resumenes(List<Orden> lista) {
        Map<Long, String> nombresPrestador = new HashMap<>();
        prestadores.todos().forEach(p -> nombresPrestador.put(p.id(), p.nombre()));
        Map<Long, AfiliadoConsulta.AfiliadoResumen> cache = new HashMap<>();
        LocalDate hoy = LocalDate.now();
        return lista.stream().map(o -> {
            var a = cache.computeIfAbsent(o.getAfiliadoId(), afiliados::obtener);
            boolean vigente = OrdenEstado.EMITIDA.name().equals(o.getEstado()) || OrdenEstado.AUTORIZADA.name().equals(o.getEstado());
            boolean vencida = vigente && o.getFechaVencimiento() != null && hoy.isAfter(o.getFechaVencimiento());
            return new OrdenDtos.OrdenResumen(o.getId(), o.getNumero(), o.getFechaEmision(), o.getEstado(), a.id(),
                    a.nroAfiliado(), a.apellidoNombre(), nombresPrestador.getOrDefault(o.getPrestadorId(), "—"),
                    o.getTotalCoseguro(), o.getFechaVencimiento(), vencida, o.getMotivoAuditoria(), o.getFechaResolucion());
        }).toList();
    }
}
