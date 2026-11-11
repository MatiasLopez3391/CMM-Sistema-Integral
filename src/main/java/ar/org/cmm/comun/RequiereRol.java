package ar.org.cmm.comun;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca los roles que podrán usar un endpoint cuando se active el login.
 *
 * <p>Hoy es solo documental. Al sumar Spring Security se reemplaza por
 * {@code @PreAuthorize("hasAnyRole(...)")} (búsqueda y reemplazo directa) o se implementa un
 * interceptor que la lea.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiereRol {
    Rol[] value();
}
