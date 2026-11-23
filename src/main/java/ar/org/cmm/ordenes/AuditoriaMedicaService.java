
package ar.org.cmm.ordenes;

import ar.org.cmm.comun.UsuarioActualProvider;
import ar.org.cmm.contratos.CargoCuotaServicio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaMedicaService {

    private final OrdenRepository ordenes;
    private final AuditoriaMedicaRepository auditorias;
    private final OrdenEstadoHistorialRepository historiales;
    private final CargoCuotaServicio cargos;
    private final UsuarioActualProvider usuarioActual;

    public AuditoriaMedicaService(
            OrdenRepository ordenes,
            AuditoriaMedicaRepository auditorias,
            OrdenEstadoHistorialRepository historiales,
            CargoCuotaServicio cargos,
            UsuarioActualProvider usuarioActual) {

        this.ordenes = ordenes;
        this.auditorias = auditorias;
        this.historiales = historiales;
        this.cargos = cargos;
        this.usuarioActual = usuarioActual;
    }

    @Transactional(readOnly = true)
    public List<OrdenDtos.OrdenResponse> pendientes() {

        return ordenes
                .findByEstadoOrderByFechaEmisionDesc(
                        OrdenEstado.PENDIENTE_AUDITORIA.name()
                )
                .stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional
    public OrdenDtos.OrdenResponse decidir(
            Long ordenId,
            String decision,
            String comentario) {

        Orden orden = ordenes.findById(ordenId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La orden no existe"
                        )
                );

        if (!OrdenEstado.PENDIENTE_AUDITORIA.name()
                .equals(orden.getEstado())) {

            throw new IllegalArgumentException(
                    "La orden no está pendiente de auditoría"
            );
        }

        if (decision == null ||
                (!"AUTORIZADA".equalsIgnoreCase(decision)
                        && !"RECHAZADA".equalsIgnoreCase(decision))) {

            throw new IllegalArgumentException(
                    "La decisión debe ser AUTORIZADA o RECHAZADA"
            );
        }

        String nuevoEstado = decision.toUpperCase(
                java.util.Locale.ROOT
        );

        String estadoAnterior = orden.getEstado();
        String usuario = usuarioActual.usuario();
        LocalDateTime ahora = LocalDateTime.now();

        AuditoriaMedica auditoria = new AuditoriaMedica();
        auditoria.setOrden(orden);
        auditoria.setUsuarioAuditor(usuario);
        auditoria.setDecision(nuevoEstado);
        auditoria.setComentario(comentario);
        auditoria.setFecha(ahora);

        auditorias.save(auditoria);

        orden.setEstado(nuevoEstado);
        ordenes.save(orden);

        OrdenEstadoHistorial historial =
                new OrdenEstadoHistorial();

        historial.setOrden(orden);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setFecha(ahora);
        historial.setUsuario(usuario);
        historial.setComentario(comentario);

        historiales.save(historial);

        // F3 registra el coseguro al emitir la orden.
        // Si el médico rechaza la orden, se anula ese cargo.
        if (OrdenEstado.RECHAZADA.name().equals(nuevoEstado)) {
            cargos.anularCoseguro(
                    orden.getNumero(),
                    "Orden rechazada por auditoría médica"
            );
        }

        return convertir(orden);
    }

    private OrdenDtos.OrdenResponse convertir(Orden orden) {

        return new OrdenDtos.OrdenResponse(
                orden.getId(),
                orden.getNumero(),
                orden.getAfiliadoId(),
                orden.getPrestadorId(),
                orden.getFechaEmision(),
                orden.getEstado(),
                orden.getTotalValorConvenido(),
                orden.getTotalCargoEntidad(),
                orden.getTotalCoseguro()
        );
    }
}
