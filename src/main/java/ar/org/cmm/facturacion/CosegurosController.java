package ar.org.cmm.facturacion;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import ar.org.cmm.contratos.CoseguroServicio.CalculoCoseguro;
import ar.org.cmm.facturacion.FacturacionDtos.AnulacionRequest;
import ar.org.cmm.facturacion.FacturacionDtos.CalculoCoseguroRequest;
import ar.org.cmm.facturacion.FacturacionDtos.CargoCuotaDto;
import ar.org.cmm.facturacion.FacturacionDtos.CargoManualRequest;
import ar.org.cmm.facturacion.FacturacionDtos.HistorialCosegurosDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Coseguros que se cobran en la cuota siguiente: carga manual, anulación e historial (T42, T43). */
@RestController
@RequestMapping("/api/facturacion/coseguros")
@RequiereRol({Rol.LIQUIDACIONES, Rol.ADMINISTRACION})
public class CosegurosController {

    private final CargoCuotaService servicio;

    public CosegurosController(CargoCuotaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public HistorialCosegurosDto historial(@RequestParam String desde, @RequestParam String hasta,
                                           @RequestParam(required = false) String estado,
                                           @RequestParam(required = false) Long afiliadoId) {
        return servicio.historial(desde, hasta, estado, afiliadoId);
    }

    @GetMapping("/afiliado/{afiliadoId}")
    public List<CargoCuotaDto> delGrupo(@PathVariable Long afiliadoId) {
        return servicio.delGrupo(afiliadoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CargoCuotaDto cargar(@Valid @RequestBody CargoManualRequest req) {
        return servicio.cargarManual(req);
    }

    @PostMapping("/{id}/anular")
    public void anular(@PathVariable Long id, @Valid @RequestBody AnulacionRequest req) {
        servicio.anular(id, req.motivo());
    }

    @PostMapping("/calcular")
    public CalculoCoseguro calcular(@Valid @RequestBody CalculoCoseguroRequest req) {
        return servicio.calcular(req);
    }
}
