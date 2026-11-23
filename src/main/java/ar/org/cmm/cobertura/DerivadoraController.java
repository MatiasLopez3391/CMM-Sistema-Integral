package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.DerivadoraDto;
import ar.org.cmm.cobertura.CoberturaDtos.DerivadoraRequest;
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
import java.util.Map;

@RestController
@RequestMapping("/api/cobertura/derivadoras")
@RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.IT})
public class DerivadoraController {

    private final DerivadoraService servicio;

    public DerivadoraController(DerivadoraService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.MESA_ENTRADAS, Rol.MEDICO_AUDITOR, Rol.IT})
    public List<DerivadoraDto> derivadoras() {
        return servicio.listarDerivadoras();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> crear(@Valid @RequestBody DerivadoraRequest req) {
        return Map.of("id", servicio.guardarDerivadora(null, req));
    }

    @PutMapping("/{id}")
    public Map<String, Long> modificar(@PathVariable Long id, @Valid @RequestBody DerivadoraRequest req) {
        return Map.of("id", servicio.guardarDerivadora(id, req));
    }
}
