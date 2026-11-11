package ar.org.cmm.comun;

/** Error de negocio esperable: se muestra tal cual al operador. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
