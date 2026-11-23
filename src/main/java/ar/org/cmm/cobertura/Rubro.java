package ar.org.cmm.cobertura;

import ar.org.cmm.comun.ReglaNegocioException;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Rubros válidos de una práctica. En la base se sigue guardando el texto (sin migración);
 * este enum solo valida y normaliza lo que entra ("Enfermería " -> "ENFERMERIA").
 */
public enum Rubro {
    CONSULTA, ENFERMERIA, LABORATORIO, IMAGENES, MEDICAMENTOS, INTERNACION, ODONTOLOGIA, OTROS;

    public static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ReglaNegocioException("El rubro es obligatorio.");
        }
        String limpio = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        for (Rubro r : values()) {
            if (r.name().equals(limpio)) {
                return r.name();
            }
        }
        throw new ReglaNegocioException("Rubro inválido: '" + texto.trim() + "'. Valores válidos: "
                + Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", ")) + ".");
    }
}
