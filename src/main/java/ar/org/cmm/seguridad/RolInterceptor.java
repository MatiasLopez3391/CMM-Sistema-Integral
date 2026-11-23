package ar.org.cmm.seguridad;

import ar.org.cmm.comun.RequiereRol;
import ar.org.cmm.comun.Rol;
import ar.org.cmm.comun.UsuarioActualProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * Hace efectivo {@link RequiereRol}: la anotación del método manda sobre la de la clase.
 * Sin anotación, alcanza con estar autenticado. Así no hace falta tocar los controladores de los otros frentes.
 */
@Component
public class RolInterceptor implements HandlerInterceptor {

    private final UsuarioActualProvider usuarioActual;
    private final RespuestasSeguridad respuestas;

    public RolInterceptor(UsuarioActualProvider usuarioActual, RespuestasSeguridad respuestas) {
        this.usuarioActual = usuarioActual;
        this.respuestas = respuestas;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        RequiereRol requisito = AnnotatedElementUtils.findMergedAnnotation(hm.getMethod(), RequiereRol.class);
        if (requisito == null) {
            requisito = AnnotatedElementUtils.findMergedAnnotation(hm.getBeanType(), RequiereRol.class);
        }
        if (requisito == null) {
            return true;
        }
        boolean permitido = Arrays.stream(requisito.value()).anyMatch(usuarioActual::tieneRol);
        if (!permitido) {
            respuestas.escribir(response, 403, "Sin permiso",
                    "Su usuario no tiene permiso para esta operación (se requiere: " + nombres(requisito.value()) + ").");
        }
        return permitido;
    }

    private static String nombres(Rol[] roles) {
        return String.join(", ", Arrays.stream(roles).map(Rol::name).toList());
    }
}
