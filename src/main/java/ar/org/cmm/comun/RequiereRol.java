package ar.org.cmm.comun;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca los roles que podrán usar un endpoint cuando se active el login.
 *
 * <p>La hace efectiva {@code ar.org.cmm.seguridad.RolInterceptor}: la anotación del método manda sobre la
 * de la clase, y un endpoint sin anotación solo exige haber iniciado sesión.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiereRol {
    Rol[] value();
}
