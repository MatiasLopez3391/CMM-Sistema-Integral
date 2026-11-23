package ar.org.cmm.cobertura;

import ar.org.cmm.comun.ReglaNegocioException;

/** Normalización de CUIT compartida por prestadores y derivadoras. */
final class Cuit {

    private Cuit() {
    }

    /** Devuelve el CUIT como XX-XXXXXXXX-X, o null si viene vacío. */
    static String normalizar(String cuit) {
        if (cuit == null || cuit.isBlank()) {
            return null;
        }
        String digitos = cuit.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            throw new ReglaNegocioException("El CUIT debe tener 11 dígitos.");
        }
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }
}
