package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.PracticaDto;
import ar.org.cmm.cobertura.CoberturaDtos.PracticaRequest;
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
@RequestMapping("/api/cobertura/practicas")
@RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.IT})
public class PracticaController {

    private final PracticaService servicio;

    public PracticaController(PracticaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.MESA_ENTRADAS, Rol.MEDICO_AUDITOR, Rol.IT})
    public List<PracticaDto> practicas() {
        return servicio.listarPracticas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PracticaDto crear(@Valid @RequestBody PracticaRequest req) {
        return servicio.crearPractica(req);
    }

    @PutMapping("/{id}")
    public PracticaDto modificar(@PathVariable Long id, @Valid @RequestBody PracticaRequest req) {
        return servicio.modificarPractica(id, req);
    }
}
