
package ar.org.cmm.ordenes;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenConsultaController {

    private final OrdenRepository ordenes;
    private final OrdenEstadoHistorialRepository historiales;

    public OrdenConsultaController(
            OrdenRepository ordenes,
            OrdenEstadoHistorialRepository historiales) {

        this.ordenes = ordenes;
        this.historiales = historiales;
    }

    @GetMapping("/{ordenId}/historial")
    @RequiereRol({
            Rol.MESA_ENTRADAS,
            Rol.MEDICO_AUDITOR,
            Rol.ADMINISTRACION
    })
    public List<HistorialResponse> historial(
            @PathVariable Long ordenId) {

        if (!ordenes.existsById(ordenId)) {
            throw new IllegalArgumentException(
                    "La orden no existe"
            );
        }

        return historiales
                .findByOrdenIdOrderByFechaAsc(ordenId)
                .stream()
                .map(h -> new HistorialResponse(
                        h.getId(),
                        h.getEstadoAnterior(),
                        h.getEstadoNuevo(),
                        h.getFecha(),
                        h.getUsuario(),
                        h.getComentario()
                ))
                .toList();
    }

    public record HistorialResponse(
            Long id,
            String estadoAnterior,
            String estadoNuevo,
            LocalDateTime fecha,
            String usuario,
            String comentario
    ) {
    }
}
