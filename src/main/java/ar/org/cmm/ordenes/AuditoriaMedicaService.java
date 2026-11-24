package ar.org.cmm.ordenes;

import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.comun.UsuarioActualProvider;
import ar.org.cmm.contratos.CargoCuotaServicio;
import ar.org.cmm.contratos.PracticaConsulta;
import ar.org.cmm.contratos.PrestadorConsulta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Buzón del médico auditor (T28, T65): autorizar, autorizar con cambios o rechazar una orden pendiente.
 *
 * <p>Autorizar con cambios es una autorización parcial: el auditor baja la cantidad de una práctica o no la autoriza
 * (cantidad 0). No puede agregar prácticas ni subir cantidades: eso es otra orden. Al rechazar o al cambiar algo el
 * motivo es obligatorio, porque se le informa al afiliado (M9).
 *
 * <p>El coseguro se registra en F3 recién al autorizar, con el importe final. Al rechazar no hay nada que anular.
 */
@Service
public class AuditoriaMedicaService {

    private final OrdenRepository ordenes;
    private final AuditoriaMedicaRepository auditorias;
    private final OrdenEstadoHistorialRepository historiales;
    private final CargoCuotaServicio cargos;
    private final UsuarioActualProvider usuarioActual;
    private final OrdenNotificacionService notificaciones;
    private final OrdenService ordenService;
    private final OrdenConsultaService consultas;
    private final PracticaConsulta practicas;
    private final PrestadorConsulta prestadores;

    public AuditoriaMedicaService(
            OrdenRepository ordenes,
            AuditoriaMedicaRepository auditorias,
            OrdenEstadoHistorialRepository historiales,
            CargoCuotaServicio cargos,
            UsuarioActualProvider usuarioActual,
            OrdenNotificacionService notificaciones,
            OrdenService ordenService,
            OrdenConsultaService consultas,
            PracticaConsulta practicas,
            PrestadorConsulta prestadores) {

        this.ordenes = ordenes;
        this.auditorias = auditorias;
        this.historiales = historiales;
        this.cargos = cargos;
        this.usuarioActual = usuarioActual;
        this.notificaciones = notificaciones;
        this.ordenService = ordenService;
        this.consultas = consultas;
        this.practicas = practicas;
        this.prestadores = prestadores;
    }

    @Transactional(readOnly = true)
    public List<OrdenDtos.OrdenResumen> pendientes() {
        return consultas.pendientesDeAuditoria();
    }

    @Transactional
    public OrdenDtos.OrdenResponse decidir(Long ordenId, String decision, String comentario) {
        return decidir(ordenId, decision, comentario, null);
    }

    @Transactional
    public OrdenDtos.OrdenResponse decidir(Long ordenId, String decision, String comentario,
                                           List<OrdenDtos.ItemDecision> cambios) {

        Orden orden = ordenes.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden", ordenId));

        if (!OrdenEstado.PENDIENTE_AUDITORIA.name().equals(orden.getEstado())) {
            throw new ReglaNegocioException("La orden " + orden.getNumero() + " no está pendiente de auditoría.");
        }
        if (decision == null
                || (!"AUTORIZADA".equalsIgnoreCase(decision) && !"RECHAZADA".equalsIgnoreCase(decision))) {
            throw new ReglaNegocioException("La decisión debe ser AUTORIZADA o RECHAZADA.");
        }

        String nuevoEstado = decision.trim().toUpperCase(Locale.ROOT);
        String motivo = comentario == null || comentario.isBlank() ? null : comentario.trim();
        boolean rechazo = OrdenEstado.RECHAZADA.name().equals(nuevoEstado);
        if (rechazo && motivo == null) {
            throw new ReglaNegocioException("Indique el motivo del rechazo: se le informa al afiliado.");
        }

        // Primero se valida todo y recién después se modifica la orden.
        List<Cambio> plan = rechazo ? List.of() : planificarCambios(orden, cambios);
        List<String> detalleCambios = plan.stream().map(Cambio::linea).toList();
        boolean parcial = !detalleCambios.isEmpty();
        if (parcial && motivo == null) {
            throw new ReglaNegocioException("Indique el motivo de los cambios: se le informa al afiliado.");
        }
        if (parcial) {
            aplicarCambios(orden, plan);
        }

        String usuario = usuarioActual.usuario();
        LocalDateTime ahora = LocalDateTime.now();
        String estadoAnterior = orden.getEstado();

        AuditoriaMedica auditoria = new AuditoriaMedica();
        auditoria.setOrden(orden);
        auditoria.setUsuarioAuditor(usuario);
        auditoria.setDecision(parcial ? "AUTORIZADA_PARCIAL" : nuevoEstado);
        auditoria.setComentario(componer(motivo, detalleCambios, 2000));
        auditoria.setFecha(ahora);
        auditorias.save(auditoria);

        orden.setEstado(nuevoEstado);
        orden.setFechaResolucion(ahora);
        if (!rechazo) {
            orden.setFechaVencimiento(LocalDate.now().plusMonths(ordenService.mesesVencimiento()));
        }
        ordenes.save(orden);

        OrdenEstadoHistorial historial = new OrdenEstadoHistorial();
        historial.setOrden(orden);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setFecha(ahora);
        historial.setUsuario(usuario);
        historial.setComentario(componer(parcial ? "Autorizada con cambios. " + motivo : motivo, detalleCambios, 1000));
        historiales.save(historial);

        if (rechazo) {
            // No debería haber cargo (las pendientes no lo generan); se anula por si existiera de una versión anterior.
            cargos.anularCoseguro(orden.getNumero(), "Orden rechazada por auditoría médica");
        } else {
            List<PracticaConsulta.PracticaResumen> practicasOrden = new ArrayList<>();
            orden.getItems().forEach(it -> practicasOrden.add(practicas.obtener(it.getPracticaId())));
            ordenService.registrarCoseguro(orden, LocalDate.now(), practicasOrden,
                    prestadores.obtener(orden.getPrestadorId()).nombre());
        }

        notificaciones.notificarDecision(orden, motivo, detalleCambios);

        return new OrdenDtos.OrdenResponse(orden.getId(), orden.getNumero(), orden.getAfiliadoId(), orden.getPrestadorId(),
                orden.getFechaEmision(), orden.getEstado(), orden.getTotalValorConvenido(), orden.getTotalCargoEntidad(),
                orden.getTotalCoseguro());
    }

    private record Cambio(OrdenItem item, int cantidad, String linea) {
    }

    /**
     * Valida las cantidades autorizadas sin tocar la orden. Devuelve una entrada por práctica cambiada
     * (ej. "Kinesiología: 10 → 5"); vacía si se autoriza lo pedido.
     */
    private List<Cambio> planificarCambios(Orden orden, List<OrdenDtos.ItemDecision> cambios) {
        if (cambios == null || cambios.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> autorizado = new HashMap<>();
        for (var c : cambios) {
            if (c == null || c.itemId() == null || c.cantidad() == null) {
                throw new ReglaNegocioException("Cada práctica necesita la cantidad autorizada.");
            }
            autorizado.put(c.itemId(), c.cantidad());
        }
        List<Cambio> plan = new ArrayList<>();
        int autorizadas = 0;
        for (OrdenItem it : orden.getItems()) {
            int pedida = it.getCantidadSolicitada() != null ? it.getCantidadSolicitada() : it.getCantidad();
            Integer nueva = autorizado.remove(it.getId());
            int cantidad = nueva != null ? nueva : it.getCantidad();
            if (cantidad < 0 || cantidad > pedida) {
                throw new ReglaNegocioException("La cantidad autorizada debe estar entre 0 y " + pedida
                        + " (lo pedido). Para agregar prácticas se emite otra orden.");
            }
            if (cantidad != it.getCantidad()) {
                String desc = practicas.obtener(it.getPracticaId()).descripcion();
                plan.add(new Cambio(it, cantidad, desc + ": " + (cantidad == 0 ? "no autorizada" : pedida + " → " + cantidad)));
            }
            if (cantidad > 0) {
                autorizadas++;
            }
        }
        if (!autorizado.isEmpty()) {
            throw new ReglaNegocioException("Hay prácticas que no pertenecen a la orden " + orden.getNumero() + ".");
        }
        if (autorizadas == 0) {
            throw new ReglaNegocioException("No autorizó ninguna práctica: en ese caso rechace la orden.");
        }
        return plan;
    }

    /** Aplica las cantidades autorizadas y recalcula los importes de cada práctica y de la orden. */
    private void aplicarCambios(Orden orden, List<Cambio> plan) {
        for (Cambio c : plan) {
            OrdenItem it = c.item();
            BigDecimal coseguroUnit = OrdenConsultaService.unitario(it.getCoseguroAfiliado(), it.getCantidad());
            BigDecimal entidadUnit = OrdenConsultaService.unitario(it.getCargoEntidad(), it.getCantidad());
            it.setCoseguroAfiliado(coseguroUnit.multiply(BigDecimal.valueOf(c.cantidad())));
            it.setCargoEntidad(entidadUnit.multiply(BigDecimal.valueOf(c.cantidad())));
            it.setCantidad(c.cantidad());
        }
        BigDecimal totalValor = BigDecimal.ZERO, totalEntidad = BigDecimal.ZERO, totalCoseguro = BigDecimal.ZERO;
        for (OrdenItem it : orden.getItems()) {
            totalValor = totalValor.add(it.getValorConvenido().multiply(BigDecimal.valueOf(it.getCantidad())));
            totalEntidad = totalEntidad.add(it.getCargoEntidad());
            totalCoseguro = totalCoseguro.add(it.getCoseguroAfiliado());
        }
        orden.setTotalValorConvenido(totalValor);
        orden.setTotalCargoEntidad(totalEntidad);
        orden.setTotalCoseguro(totalCoseguro);
    }

    private static String componer(String motivo, List<String> cambios, int max) {
        StringBuilder sb = new StringBuilder(motivo == null ? "" : motivo);
        if (!cambios.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append(String.join("; ", cambios));
        }
        String t = sb.toString();
        return t.isEmpty() ? null : (t.length() > max ? t.substring(0, max) : t);
    }
}
