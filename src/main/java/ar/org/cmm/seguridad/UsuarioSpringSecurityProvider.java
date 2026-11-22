package ar.org.cmm.seguridad;

import ar.org.cmm.comun.Rol;
import ar.org.cmm.comun.UsuarioActualProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Usuario actual tomado de la sesión de Spring Security (reemplaza al provisorio {@code UsuarioDemoProvider}).
 * Fuera de una sesión autenticada (carga de demostración, pruebas, tareas internas) responde "sistema" y sin roles.
 */
@Component
public class UsuarioSpringSecurityProvider implements UsuarioActualProvider {

    static final String USUARIO_SISTEMA = "sistema";

    @Override
    public String usuario() {
        UsuarioAutenticado u = actual();
        return u != null ? u.getUsername() : USUARIO_SISTEMA;
    }

    @Override
    public Set<Rol> roles() {
        UsuarioAutenticado u = actual();
        return u != null ? u.getRoles() : Set.of();
    }

    private UsuarioAutenticado actual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UsuarioAutenticado u) {
            return u;
        }
        return null;
    }
}
