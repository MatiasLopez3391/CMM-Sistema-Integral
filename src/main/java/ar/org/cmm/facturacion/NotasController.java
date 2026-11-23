package ar.org.cmm.facturacion;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import ar.org.cmm.facturacion.FacturacionDtos.NotaRequest;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoNota;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Notas de crédito y débito internas (T46). */
@RestController
@RequestMapping("/api/facturacion/notas")
@RequiereRol({Rol.LIQUIDACIONES, Rol.ADMINISTRACION})
public class NotasController {

    private final NotaService servicio;

    public NotasController(NotaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultadoNota emitir(@Valid @RequestBody NotaRequest req) {
        return servicio.emitir(req);
    }
}
