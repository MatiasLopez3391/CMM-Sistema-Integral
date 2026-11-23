
package ar.org.cmm.ordenes;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService ordenService;

    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @PostMapping
    @RequiereRol({Rol.MESA_ENTRADAS})
    public ResponseEntity<OrdenDtos.OrdenResponse> emitir(
            @Valid @RequestBody OrdenDtos.EmitirOrdenRequest request) {

        OrdenDtos.OrdenResponse respuesta =
                ordenService.emitir(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }
}
