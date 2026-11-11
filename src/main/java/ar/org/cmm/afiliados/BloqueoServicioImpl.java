package ar.org.cmm.afiliados;

import ar.org.cmm.afiliados.Afiliado.EstadoAfiliado;
import ar.org.cmm.afiliados.AfiliadoDtos.BloqueoDto;
import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.comun.UsuarioActualProvider;
import ar.org.cmm.contratos.BloqueoServicio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bloqueo manual con registro (H4). El bloqueo se guarda en el titular y alcanza a todo el grupo:
 * siguen activos en la obra social pero no pueden pedir órdenes.
 */
@Service
public class BloqueoServicioImpl implements BloqueoServicio {

    public static final String MARCA_FALTA_PAGO = "Bloqueado por falta de pago";

    private final AfiliadoRepository afiliados;
    private final BloqueoHistorialRepository historial;
    private final UsuarioActualProvider usuarioActual;
    private final AuditoriaService auditoria;

    public BloqueoServicioImpl(AfiliadoRepository afiliados, BloqueoHistorialRepository historial,
                               UsuarioActualProvider usuarioActual, AuditoriaService auditoria) {
        this.afiliados = afiliados;
        this.historial = historial;
        this.usuarioActual = usuarioActual;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaBloqueado(Long afiliadoId) {
        return buscar(afiliadoId).titularDelGrupo().isBloqueado();
    }

    @Override
    @Transactional
    public void bloquear(Long afiliadoId, String marca, String motivo) {
        Afiliado titular = buscar(afiliadoId).titularDelGrupo();
        if (titular.getEstado() == EstadoAfiliado.BAJA) {
            throw new ReglaNegocioException("No se puede bloquear un afiliado dado de baja.");
        }
        if (titular.isBloqueado()) {
            throw new ReglaNegocioException("El grupo familiar ya está bloqueado.");
        }
        titular.setBloqueado(true);
        registrar(titular, "BLOQUEO", marca == null || marca.isBlank() ? MARCA_FALTA_PAGO : marca.trim(), motivo);
    }

    @Override
    @Transactional
    public void desbloquear(Long afiliadoId, String motivo) {
        Afiliado titular = buscar(afiliadoId).titularDelGrupo();
        if (!titular.isBloqueado()) {
            throw new ReglaNegocioException("El grupo familiar no está bloqueado.");
        }
        titular.setBloqueado(false);
        registrar(titular, "DESBLOQUEO", null, motivo);
    }

    @Transactional(readOnly = true)
    public List<BloqueoDto> historial(Long afiliadoId) {
        Afiliado titular = buscar(afiliadoId).titularDelGrupo();
        return historial.findByAfiliadoIdOrderByFechaDesc(titular.getId()).stream().map(BloqueoDto::de).toList();
    }

    private void registrar(Afiliado titular, String accion, String marca, String motivo) {
        BloqueoHistorial h = new BloqueoHistorial();
        h.setAfiliado(titular);
        h.setAccion(accion);
        h.setMarca(marca);
        h.setMotivo(motivo == null || motivo.isBlank() ? "(sin motivo)" : motivo.trim());
        h.setUsuario(usuarioActual.usuario());
        h.setFecha(LocalDateTime.now());
        historial.save(h);
        auditoria.registrar("afiliado", titular.getId(), accion, (marca != null ? marca + ": " : "") + h.getMotivo());
    }

    private Afiliado buscar(Long id) {
        return afiliados.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Afiliado", id));
    }
}
