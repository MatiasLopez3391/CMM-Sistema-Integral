package ar.org.cmm.cobertura.importacion;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Detecta, por el nombre del encabezado (sin importar mayúsculas ni tildes), qué columna es cada dato:
 * "cmm", "nomenclador", "descripcion", "rubro" y "valor". Las columnas "a cargo del afiliado" se ignoran.
 */
public final class DetectorColumnas {

    private DetectorColumnas() {
    }

    public static Map<String, Integer> detectar(List<String> fila) {
        Map<String, Integer> col = new HashMap<>();
        Integer ultimaValores = null;
        for (int i = 0; i < fila.size(); i++) {
            String t = normalizar(fila.get(i));
            if (t.isEmpty()) {
                continue;
            }
            if (t.contains("cargo")) {
                continue; // "A cargo de Cimesa" / "A cargo del afiliado": no se importan
            }
            if (t.equals("cmm") || t.equals("codigo cmm") || t.equals("cod cmm")) {
                col.put("cmm", i);
            } else if (t.contains("nomenclador")) {
                col.put("nomenclador", i);
            } else if (t.equals("codigo") || t.equals("cod")) {
                col.putIfAbsent("cmm_alt", i);
            } else if (t.contains("detalle") || t.equals("practica") || t.contains("descripcion")) {
                col.putIfAbsent("descripcion", i);
            } else if (t.equals("rubro")) {
                col.put("rubro", i);
            } else if (t.equals("valor") || t.equals("importe") || t.equals("precio")) {
                col.put("valor", i);
            } else if (t.startsWith("valores") || t.startsWith("valor ")) {
                ultimaValores = i; // la columna "Valores ..." más a la derecha es la más nueva
            }
        }
        if (!col.containsKey("valor") && ultimaValores != null) {
            col.put("valor", ultimaValores);
        }
        if (!col.containsKey("cmm") && col.containsKey("cmm_alt")) {
            col.put("cmm", col.get("cmm_alt"));
        } else if (col.containsKey("cmm_alt") && !col.containsKey("nomenclador")) {
            // Planilla del prestador: columna "CMM" + columna "Código" (= código de nomenclador)
            col.put("nomenclador", col.get("cmm_alt"));
        }
        return col;
    }

    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
