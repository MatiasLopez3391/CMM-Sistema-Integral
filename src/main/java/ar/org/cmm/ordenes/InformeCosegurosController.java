
package ar.org.cmm.ordenes;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ordenes/informe-coseguros")
@RequiereRol({
        Rol.ADMINISTRACION,
        Rol.LIQUIDACIONES
})
public class InformeCosegurosController {

    private final InformeCosegurosService servicio;

    public InformeCosegurosController(
            InformeCosegurosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public InformeCosegurosService.InformeResponse generar(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam(required = false)
            Long afiliadoId) {

        return servicio.generar(desde, hasta, afiliadoId);
    }
}
