package ar.org.cmm.cobranza;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Lee un archivo de cobranza según su {@link LayoutArchivo}. No accede a la base. */
@Component
public class LectorArchivoCobro {

    public record RegistroLeido(int linea, String nroAfiliado, LocalDate fecha, BigDecimal importe, String referencia, String error) {
        public boolean valido() {
            return error == null;
        }
    }

    public List<RegistroLeido> leer(String contenido, LayoutArchivo layout) {
        List<RegistroLeido> res = new ArrayList<>();
        String[] lineas = contenido.replace("﻿", "").split("\\r?\\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(layout.getFechaFormato());
        for (int i = 0; i < lineas.length; i++) {
            int nroLinea = i + 1;
            String linea = lineas[i];
            if (i < layout.getLineasEncabezado() || linea.isBlank()) {
                continue;
            }
            if (layout.getPrefijoRegistro() != null && !layout.getPrefijoRegistro().isBlank()
                    && !linea.startsWith(layout.getPrefijoRegistro())) {
                continue; // encabezado / pie / otros tipos de registro
            }
            try {
                String afiliado, fechaTxt, importeTxt, referencia;
                if (LayoutArchivo.DELIMITADO.equals(layout.getFormato())) {
                    String[] cols = linea.split(Pattern.quote(layout.getDelimitador() == null ? ";" : layout.getDelimitador()), -1);
                    afiliado = columna(cols, layout.getAfiliadoPos());
                    fechaTxt = columna(cols, layout.getFechaPos());
                    importeTxt = columna(cols, layout.getImportePos());
                    referencia = layout.getReferenciaPos() == null ? null : columna(cols, layout.getReferenciaPos());
                } else {
                    afiliado = tramo(linea, layout.getAfiliadoPos(), layout.getAfiliadoLargo());
                    fechaTxt = tramo(linea, layout.getFechaPos(), layout.getFechaLargo());
                    importeTxt = tramo(linea, layout.getImportePos(), layout.getImporteLargo());
                    referencia = layout.getReferenciaPos() == null ? null : tramo(linea, layout.getReferenciaPos(), layout.getReferenciaLargo());
                }
                LocalDate fecha = LocalDate.parse(fechaTxt.trim(), fmt);
                BigDecimal importe = importe(importeTxt, layout.getImporteDecimalesImpl());
                res.add(new RegistroLeido(nroLinea, afiliado.trim(), fecha, importe,
                        referencia == null || referencia.isBlank() ? null : referencia.trim(), null));
            } catch (DateTimeParseException e) {
                res.add(new RegistroLeido(nroLinea, null, null, null, null, "Fecha inválida"));
            } catch (NumberFormatException e) {
                res.add(new RegistroLeido(nroLinea, null, null, null, null, "Importe inválido"));
            } catch (IndexOutOfBoundsException e) {
                res.add(new RegistroLeido(nroLinea, null, null, null, null, "Línea más corta que el diseño de archivo"));
            }
        }
        return res;
    }

    private static String tramo(String linea, int pos, Integer largo) {
        if (largo == null) {
            throw new IndexOutOfBoundsException("largo no definido");
        }
        return linea.substring(pos - 1, pos - 1 + largo);
    }

    private static String columna(String[] cols, int pos) {
        return cols[pos - 1].replaceAll("^\"|\"$", "");
    }

    static BigDecimal importe(String texto, int decimalesImplicitos) {
        String t = texto.trim();
        if (decimalesImplicitos > 0) {
            return new BigDecimal(t.replaceAll("\\D", "")).movePointLeft(decimalesImplicitos).setScale(2, RoundingMode.HALF_UP);
        }
        t = t.replace("$", "").replace(" ", "");
        int coma = t.lastIndexOf(','), punto = t.lastIndexOf('.');
        if (coma > punto) {
            t = t.replace(".", "").replace(',', '.');
        } else if (punto > coma && coma >= 0) {
            t = t.replace(",", "");
        }
        return new BigDecimal(t).setScale(2, RoundingMode.HALF_UP);
    }
}
