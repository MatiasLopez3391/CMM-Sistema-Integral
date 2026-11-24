package ar.org.cmm.ordenes;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Emisión y anulación de órdenes (Mesa de Entradas). */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService ordenService;
    private final OrdenConsultaService consultas;

    public OrdenController(OrdenService ordenService, OrdenConsultaService consultas) {
        this.ordenService = ordenService;
        this.consultas = consultas;
    }

    @PostMapping
    @RequiereRol({Rol.MESA_ENTRADAS})
    public ResponseEntity<OrdenDtos.OrdenResponse> emitir(@Valid @RequestBody OrdenDtos.EmitirOrdenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.emitir(request));
    }

    /** Calcula coseguro y auditoría sin guardar nada. */
    @PostMapping("/simular")
    @RequiereRol({Rol.MESA_ENTRADAS})
    public OrdenDtos.SimulacionResponse simular(@Valid @RequestBody OrdenDtos.EmitirOrdenRequest request) {
        return ordenService.simular(request);
    }

    @PostMapping("/{ordenId}/anular")
    @RequiereRol({Rol.MESA_ENTRADAS, Rol.ADMINISTRACION})
    public OrdenDtos.AnulacionResponse anular(@PathVariable Long ordenId, @Valid @RequestBody OrdenDtos.AnularRequest request) {
        var r = ordenService.anular(ordenId, request.motivo());
        return new OrdenDtos.AnulacionResponse(consultas.resumen(ordenId), r.aviso());
    }
}
