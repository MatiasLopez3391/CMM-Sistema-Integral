package ar.org.cmm.comun;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.EnumSet;
import java.util.Set;

/**
 * Implementación provisoria SIN autenticación.
 *
 * <p>Toma el nombre del encabezado HTTP {@code X-Usuario} (el frontend lo envía con el operador
 * elegido en pantalla) para que la auditoría ya registre nombres. Si no hay encabezado, usa
 * "sistema". Otorga todos los roles.
 *
 * <p>Para activar el login: crear {@code UsuarioSpringSecurityProvider implements UsuarioActualProvider}
 * anotado con {@code @Primary}, que lea {@code SecurityContextHolder.getContext().getAuthentication()}.
 */
@Component
public class UsuarioDemoProvider implements UsuarioActualProvider {

    static final String ENCABEZADO = "X-Usuario";

    @Override
    public String usuario() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String valor = request.getHeader(ENCABEZADO);
            if (valor != null && !valor.isBlank()) {
                try {
                    valor = java.net.URLDecoder.decode(valor, java.nio.charset.StandardCharsets.UTF_8).trim();
                } catch (IllegalArgumentException e) {
                    valor = valor.trim();
                }
                return valor.length() > 80 ? valor.substring(0, 80) : valor;
            }
        }
        return "sistema";
    }

    @Override
    public Set<Rol> roles() {
        return EnumSet.allOf(Rol.class);
    }
}
