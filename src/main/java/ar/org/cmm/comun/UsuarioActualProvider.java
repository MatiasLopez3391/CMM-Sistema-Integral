package ar.org.cmm.comun;

import java.util.Set;

/**
 * Único punto de acceso al usuario que opera el sistema.
 *
 * <p>Regla del plan de trabajo: ningún módulo lee el usuario por su cuenta. La implementación
 * es {@code ar.org.cmm.seguridad.UsuarioSpringSecurityProvider}, que lee la sesión de Spring Security.
 */
public interface UsuarioActualProvider {

    /** Nombre de usuario para auditoría. Nunca null. */
    String usuario();

    /** Roles del usuario actual. */
    Set<Rol> roles();

    default boolean tieneRol(Rol rol) {
        return roles().contains(rol);
    }
}
