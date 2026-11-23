package ar.org.cmm.cobertura.importacion;

import java.math.BigDecimal;

/** Interpreta importes en formato argentino ($28.118,34) o estadounidense ($28,118.34). */
public final class ParserImporte {

    private ParserImporte() {
    }

    /** Devuelve el importe, o null si el texto no es un número. */
    public static BigDecimal parsear(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.replace("$", "").replace(" ", "").trim();
        if (t.isEmpty()) {
            return null;
        }
        int ultimaComa = t.lastIndexOf(',');
        int ultimoPunto = t.lastIndexOf('.');
        if (ultimaComa > ultimoPunto) {
            t = t.replace(".", "").replace(',', '.');      // 28.118,34
        } else if (ultimoPunto > ultimaComa && ultimaComa >= 0) {
            t = t.replace(",", "");                         // 28,118.34
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
