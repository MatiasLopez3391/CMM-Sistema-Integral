
package ar.org.cmm.ordenes;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria-medica")
@RequiereRol({Rol.MEDICO_AUDITOR})
public class AuditoriaMedicaController {

    private final AuditoriaMedicaService servicio;

    public AuditoriaMedicaController(
            AuditoriaMedicaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/pendientes")
    public List<OrdenDtos.OrdenResponse> pendientes() {
        return servicio.pendientes();
    }

    @PostMapping("/{ordenId}/decision")
    public OrdenDtos.OrdenResponse decidir(
            @PathVariable Long ordenId,
            @Valid @RequestBody DecisionRequest request) {

        return servicio.decidir(
                ordenId,
                request.decision(),
                request.comentario()
        );
    }

    public record DecisionRequest(
            @NotBlank String decision,
            String comentario
    ) {
    }
}
