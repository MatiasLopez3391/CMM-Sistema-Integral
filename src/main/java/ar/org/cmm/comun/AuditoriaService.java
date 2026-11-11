package ar.org.cmm.comun;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    private final AuditoriaRepository repo;
    private final UsuarioActualProvider usuarioActual;

    public AuditoriaService(AuditoriaRepository repo, UsuarioActualProvider usuarioActual) {
        this.repo = repo;
        this.usuarioActual = usuarioActual;
    }

    @Transactional
    public void registrar(String entidad, Long entidadId, String accion, String detalle) {
        AuditoriaCambio a = new AuditoriaCambio();
        a.setEntidad(entidad);
        a.setEntidadId(entidadId);
        a.setAccion(accion);
        a.setDetalle(detalle != null && detalle.length() > 1000 ? detalle.substring(0, 1000) : detalle);
        a.setUsuario(usuarioActual.usuario());
        a.setFecha(LocalDateTime.now());
        repo.save(a);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaCambio> deEntidad(String entidad, Long id) {
        return repo.findByEntidadAndEntidadId(entidad, id, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "fecha")));
    }
}
