package ar.org.cmm.historiaclinica;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.AfiliadoConsulta.AfiliadoResumen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Historia clínica del afiliado y de su grupo familiar.
 * Con el login activo, solo la verán los roles MEDICO_AUDITOR y MESA_ENTRADAS (y se registra cada consulta).
 */
@RestController
@RequestMapping("/api/afiliados/{afiliadoId}/historia-clinica")
@RequiereRol({Rol.MEDICO_AUDITOR, Rol.MESA_ENTRADAS})
public class HistoriaClinicaController {

    private final HistoriaClinicaRepository repo;
    private final AfiliadoConsulta afiliados;
    private final AuditoriaService auditoria;

    public HistoriaClinicaController(HistoriaClinicaRepository repo, AfiliadoConsulta afiliados, AuditoriaService auditoria) {
        this.repo = repo;
        this.afiliados = afiliados;
        this.auditoria = auditoria;
    }

    public record EntradaDto(Long id, Long afiliadoId, String afiliado, LocalDate fecha, String tipo, String profesional,
                             String diagnostico, String detalle, String registradoPor, LocalDateTime registradoEl) {
    }

    public record EntradaRequest(@NotNull @PastOrPresent LocalDate fecha,
                                 @NotBlank String tipo,
                                 @Size(max = 120) String profesional,
                                 @Size(max = 200) String diagnostico,
                                 @NotBlank @Size(max = 4000) String detalle) {
    }

    @GetMapping
    @Transactional
    public List<EntradaDto> listar(@PathVariable Long afiliadoId, @RequestParam(defaultValue = "false") boolean grupo) {
        AfiliadoResumen af = afiliados.obtener(afiliadoId);
        List<AfiliadoResumen> alcance = grupo ? afiliados.integrantesActivos(af.idTitular()) : List.of(af);
        Map<Long, String> nombres = new HashMap<>();
        alcance.forEach(x -> nombres.put(x.id(), x.apellidoNombre()));
        nombres.putIfAbsent(af.id(), af.apellidoNombre());
        auditoria.registrar("historia_clinica", afiliadoId, "CONSULTA", grupo ? "Grupo familiar" : "Individual");
        return repo.findByAfiliadoIdInOrderByFechaDescIdDesc(nombres.keySet()).stream()
                .map(e -> new EntradaDto(e.getId(), e.getAfiliadoId(), nombres.get(e.getAfiliadoId()), e.getFecha(), e.getTipo(),
                        e.getProfesional(), e.getDiagnostico(), e.getDetalle(), e.getCreatedBy(), e.getCreatedAt()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Long> agregar(@PathVariable Long afiliadoId, @Valid @RequestBody EntradaRequest req) {
        AfiliadoResumen af = afiliados.obtener(afiliadoId);
        if (!"ACTIVO".equals(af.estado())) {
            throw new ReglaNegocioException("El afiliado está dado de baja.");
        }
        HistoriaClinicaEntrada e = new HistoriaClinicaEntrada();
        e.setAfiliadoId(afiliadoId);
        e.setFecha(req.fecha());
        e.setTipo(req.tipo().trim().toUpperCase());
        e.setProfesional(req.profesional());
        e.setDiagnostico(req.diagnostico());
        e.setDetalle(req.detalle().trim());
        Long id = repo.save(e).getId();
        auditoria.registrar("historia_clinica", afiliadoId, "ALTA_ENTRADA", e.getTipo() + " " + e.getFecha());
        return Map.of("id", id);
    }
}
