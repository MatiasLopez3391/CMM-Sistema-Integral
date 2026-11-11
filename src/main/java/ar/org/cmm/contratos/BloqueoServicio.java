package ar.org.cmm.contratos;

/**
 * Contrato publicado por F1. El bloqueo es manual (H4) y aplica a todo el grupo familiar:
 * el afiliado sigue activo pero no puede pedir órdenes (lo controla F5).
 */
public interface BloqueoServicio {

    boolean estaBloqueado(Long afiliadoId);

    void bloquear(Long afiliadoId, String marca, String motivo);

    void desbloquear(Long afiliadoId, String motivo);
}
