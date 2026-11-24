package ar.org.cmm.ordenes;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Buzón del médico auditor. */
@RestController
@RequestMapping("/api/auditoria-medica")
@RequiereRol({Rol.MEDICO_AUDITOR})
public class AuditoriaMedicaController {

    private final AuditoriaMedicaService servicio;
    private final OrdenConsultaService consultas;

    public AuditoriaMedicaController(AuditoriaMedicaService servicio, OrdenConsultaService consultas) {
        this.servicio = servicio;
        this.consultas = consultas;
    }

    @GetMapping("/pendientes")
    public List<OrdenDtos.OrdenResumen> pendientes() {
        return servicio.pendientes();
    }

    /** Últimas órdenes que resolvió la auditoría. */
    @GetMapping("/resueltas")
    public List<OrdenDtos.OrdenResumen> resueltas() {
        return consultas.ultimasResueltas();
    }

    @PostMapping("/{ordenId}/decision")
    public OrdenDtos.OrdenResponse decidir(@PathVariable Long ordenId, @Valid @RequestBody OrdenDtos.DecisionRequest request) {
        return servicio.decidir(ordenId, request.decision(), request.comentario(), request.items());
    }
}
