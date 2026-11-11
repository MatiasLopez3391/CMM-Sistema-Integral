package ar.org.cmm.afiliados;

import ar.org.cmm.afiliados.Afiliado.EstadoAfiliado;
import ar.org.cmm.afiliados.AfiliadoDtos.AfiliadoFicha;
import ar.org.cmm.afiliados.AfiliadoDtos.AfiliadoFila;
import ar.org.cmm.afiliados.AfiliadoDtos.AfiliadoRequest;
import ar.org.cmm.afiliados.AfiliadoDtos.AreaDto;
import ar.org.cmm.afiliados.AfiliadoDtos.BajaRequest;
import ar.org.cmm.afiliados.AfiliadoDtos.BloqueoDto;
import ar.org.cmm.afiliados.AfiliadoDtos.BloqueoRequest;
import ar.org.cmm.afiliados.AfiliadoDtos.IntegranteRequest;
import ar.org.cmm.afiliados.AfiliadoDtos.PaginaDto;
import ar.org.cmm.comun.AuditoriaCambio;
import ar.org.cmm.comun.AuditoriaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AfiliadoController {

    private final AfiliadoService servicio;
    private final BloqueoServicioImpl bloqueo;
    private final AuditoriaService auditoria;

    public AfiliadoController(AfiliadoService servicio, BloqueoServicioImpl bloqueo, AuditoriaService auditoria) {
        this.servicio = servicio;
        this.bloqueo = bloqueo;
        this.auditoria = auditoria;
    }

    @GetMapping("/afiliados")
    @RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES, Rol.MESA_ENTRADAS, Rol.MEDICO_AUDITOR})
    public PaginaDto<AfiliadoFila> buscar(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) EstadoAfiliado estado,
                                          @RequestParam(required = false) Long planId,
                                          @RequestParam(defaultValue = "false") boolean soloTitulares,
                                          @RequestParam(required = false) Boolean bloqueado,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "25") int tamanio) {
        return servicio.buscar(q, estado, planId, soloTitulares, bloqueado, pagina, tamanio);
    }

    @GetMapping("/afiliados/{id}")
    public AfiliadoFicha ficha(@PathVariable Long id) {
        return servicio.ficha(id);
    }

    @PostMapping("/afiliados")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol({Rol.ADMINISTRACION})
    public Map<String, Long> crear(@Valid @RequestBody AfiliadoRequest req) {
        return Map.of("id", servicio.crearTitular(req));
    }

    @PutMapping("/afiliados/{id}")
    @RequiereRol({Rol.ADMINISTRACION})
    public void modificar(@PathVariable Long id, @Valid @RequestBody AfiliadoRequest req) {
        servicio.modificar(id, req);
    }

    @PostMapping("/afiliados/{id}/integrantes")
    @ResponseStatus(HttpStatus.CREATED)
    @RequiereRol({Rol.ADMINISTRACION})
    public Map<String, Long> agregarIntegrante(@PathVariable Long id, @Valid @RequestBody IntegranteRequest req) {
        return Map.of("id", servicio.agregarIntegrante(id, req));
    }

    @PostMapping("/afiliados/{id}/baja")
    @RequiereRol({Rol.ADMINISTRACION})
    public Map<String, Integer> baja(@PathVariable Long id, @Valid @RequestBody BajaRequest req) {
        return Map.of("integrantesDadosDeBaja", servicio.darDeBaja(id, req));
    }

    // ---- Bloqueo manual (H4)
    @PostMapping("/afiliados/{id}/bloqueo")
    @RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES})
    public void bloquear(@PathVariable Long id, @Valid @RequestBody BloqueoRequest req) {
        bloqueo.bloquear(id, req.marca(), req.motivo());
    }

    @PostMapping("/afiliados/{id}/desbloqueo")
    @RequiereRol({Rol.ADMINISTRACION, Rol.LIQUIDACIONES})
    public void desbloquear(@PathVariable Long id, @Valid @RequestBody BloqueoRequest req) {
        bloqueo.desbloquear(id, req.motivo());
    }

    @GetMapping("/afiliados/{id}/bloqueos")
    public List<BloqueoDto> historialBloqueos(@PathVariable Long id) {
        return bloqueo.historial(id);
    }

    @GetMapping("/afiliados/{id}/auditoria")
    @RequiereRol({Rol.ADMINISTRACION, Rol.IT})
    public List<AuditoriaCambio> auditoria(@PathVariable Long id) {
        return auditoria.deEntidad("afiliado", id);
    }

    @GetMapping("/areas")
    public List<AreaDto> areas() {
        return servicio.areas();
    }
}
