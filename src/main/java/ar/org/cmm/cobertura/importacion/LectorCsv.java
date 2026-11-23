package ar.org.cmm.cobertura.importacion;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Lee CSV con ';' o ','. Es el lector por defecto: toma cualquier archivo que no sea Excel. */
@Component
@Order(2)
public class LectorCsv implements LectorPlanilla {

    @Override
    public boolean soporta(String nombreArchivo) {
        return true;
    }

    @Override
    public List<List<String>> leer(InputStream in) throws IOException {
        List<List<String>> filas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linea;
            Character sep = null;
            while ((linea = br.readLine()) != null) {
                if (linea.startsWith("\uFEFF")) {
                    linea = linea.substring(1);
                }
                if (sep == null && !linea.isBlank()) {
                    sep = linea.chars().filter(ch -> ch == ';').count() >= linea.chars().filter(ch -> ch == ',').count() ? ';' : ',';
                }
                List<String> fila = new ArrayList<>();
                for (String parte : linea.split(Pattern.quote(String.valueOf(sep == null ? ';' : sep)), -1)) {
                    fila.add(parte.replaceAll("^\"|\"$", "").trim());
                }
                filas.add(fila);
            }
        }
        return filas;
    }
}
