package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.PlanDto;
import ar.org.cmm.cobertura.CoberturaDtos.PlanRequest;
import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cobertura/planes")
@RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.IT})
public class PlanController {

    private final PlanService servicio;

    public PlanController(PlanService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.MESA_ENTRADAS, Rol.MEDICO_AUDITOR, Rol.IT})
    public List<PlanDto> planes() {
        return servicio.listarPlanes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanDto crear(@Valid @RequestBody PlanRequest req) {
        return servicio.guardarPlan(null, req);
    }

    @PutMapping("/{id}")
    public PlanDto modificar(@PathVariable Long id, @Valid @RequestBody PlanRequest req) {
        return servicio.guardarPlan(id, req);
    }
}
