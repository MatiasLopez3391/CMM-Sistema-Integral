package ar.org.cmm.ordenes;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Consulta de órdenes: listado, detalle con historial y aviso de órdenes resueltas por auditoría. */
@RestController
@RequestMapping("/api/ordenes")
@RequiereRol({Rol.MESA_ENTRADAS, Rol.MEDICO_AUDITOR, Rol.ADMINISTRACION, Rol.LIQUIDACIONES})
public class OrdenConsultaController {

    private final OrdenConsultaService consultas;

    public OrdenConsultaController(OrdenConsultaService consultas) {
        this.consultas = consultas;
    }

    @GetMapping
    public List<OrdenDtos.OrdenResumen> buscar(
            @RequestParam(required = false) Long afiliadoId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return consultas.buscar(afiliadoId, estado, numero, desde, hasta);
    }

    @GetMapping("/{ordenId}")
    public OrdenDtos.OrdenDetalle detalle(@PathVariable Long ordenId) {
        return consultas.detalle(ordenId);
    }

    @GetMapping("/{ordenId}/historial")
    public List<OrdenDtos.HistorialEstado> historial(@PathVariable Long ordenId) {
        return consultas.detalle(ordenId).historial();
    }

    /** Órdenes resueltas por el auditor después de {@code desde}: Mesa de Entradas las consulta cada minuto. */
    @GetMapping("/avisos")
    @RequiereRol({Rol.MESA_ENTRADAS, Rol.ADMINISTRACION})
    public List<OrdenDtos.AvisoResolucion> avisos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde) {
        return consultas.resueltasDesde(desde);
    }
}
