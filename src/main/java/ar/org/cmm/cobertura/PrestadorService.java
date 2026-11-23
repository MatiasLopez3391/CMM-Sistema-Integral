package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.PrestadorDto;
import ar.org.cmm.cobertura.CoberturaDtos.PrestadorRequest;
import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** ABM de prestadores (clínicas, instituciones, profesionales). */
@Service
public class PrestadorService {

    private final PrestadorRepository prestadores;
    private final AuditoriaService auditoria;

    public PrestadorService(PrestadorRepository prestadores, AuditoriaService auditoria) {
        this.prestadores = prestadores;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<PrestadorDto> listarPrestadores() {
        return prestadores.findAllByOrderByNombreAsc().stream().map(PrestadorDto::de).toList();
    }

    @Transactional
    public PrestadorDto guardarPrestador(Long id, PrestadorRequest req) {
        Prestador p = id == null ? new Prestador() : prestadores.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Prestador", id));
        p.setNombre(req.nombre().trim());
        p.setCuit(Cuit.normalizar(req.cuit()));
        p.setTipo(req.tipo().trim().toUpperCase());
        p.setEmail(req.email() == null || req.email().isBlank() ? null : req.email().trim());
        p.setActivo(id == null || req.activo());
        Prestador guardado = prestadores.save(p);
        auditoria.registrar("prestador", guardado.getId(), id == null ? "ALTA" : "MODIFICACION", guardado.getNombre());
        return PrestadorDto.de(guardado);
    }
}
