package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.PracticaDto;
import ar.org.cmm.cobertura.CoberturaDtos.PracticaRequest;
import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** ABM de prácticas del nomenclador. */
@Service
public class PracticaService {

    private final PracticaRepository practicas;
    private final AuditoriaService auditoria;

    public PracticaService(PracticaRepository practicas, AuditoriaService auditoria) {
        this.practicas = practicas;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<PracticaDto> listarPracticas() {
        return practicas.findAllByOrderByCodigoCmmAsc().stream().map(PracticaDto::de).toList();
    }

    @Transactional
    public PracticaDto crearPractica(PracticaRequest req) {
        validarCodigoLibre(req.codigoCmm(), null);
        Practica guardada = practicas.save(aplicar(new Practica(), req));
        auditoria.registrar("practica", guardada.getId(), "ALTA", guardada.getCodigoCmm() + " " + guardada.getDescripcion());
        return PracticaDto.de(guardada);
    }

    @Transactional
    public PracticaDto modificarPractica(Long id, PracticaRequest req) {
        Practica p = practicas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Práctica", id));
        validarCodigoLibre(req.codigoCmm(), id);
        Practica guardada = practicas.save(aplicar(p, req));
        auditoria.registrar("practica", id, "MODIFICACION", guardada.getCodigoCmm() + " " + guardada.getDescripcion());
        return PracticaDto.de(guardada);
    }

    private void validarCodigoLibre(String codigoCmm, Long idActual) {
        practicas.findByCodigoCmm(codigoCmm.trim())
                .filter(otra -> !otra.getId().equals(idActual))
                .ifPresent(otra -> {
                    throw new ReglaNegocioException("Ya existe la práctica " + codigoCmm);
                });
    }

    private Practica aplicar(Practica p, PracticaRequest req) {
        p.setCodigoCmm(req.codigoCmm().trim());
        p.setCodigoNomenclador(req.codigoNomenclador());
        p.setDescripcion(req.descripcion().trim());
        p.setRubro(Rubro.normalizar(req.rubro()));
        p.setRequiereAuditoria(req.requiereAuditoria());
        return p;
    }
}
