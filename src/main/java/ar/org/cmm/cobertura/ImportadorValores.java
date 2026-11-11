package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.CoberturaDtos.ResultadoImportacion;
import ar.org.cmm.cobertura.CoberturaDtos.ValorRequest;
import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Importa la planilla de valores que envía cada prestador (Excel .xlsx/.xls o CSV con ';' o ',').
 *
 * <p>Detecta la fila de encabezados buscando columnas por nombre (sin importar mayúsculas ni tildes):
 * <ul>
 *   <li>código CMM: "CMM", "Código CMM" o "Código" (obligatoria)</li>
 *   <li>código de nomenclador: "Nomenclador" o "Código nomenclador" (opcional)</li>
 *   <li>descripción: "Detalle práctica", "Práctica" o "Descripción" (para crear prácticas nuevas)</li>
 *   <li>valor: "Valor", "Importe" o la columna "Valores ..." más a la derecha (obligatoria)</li>
 *   <li>rubro: "Rubro" (opcional; por defecto OTROS)</li>
 * </ul>
 * Las columnas "a cargo del afiliado" se ignoran: el coseguro se calcula según el plan (H12).
 */
@Service
public class ImportadorValores {

    private final CoberturaService cobertura;
    private final PracticaRepository practicas;
    private final PrestadorRepository prestadores;
    private final ValorConvenidoRepository valores;
    private final AuditoriaService auditoria;

    public ImportadorValores(CoberturaService cobertura, PracticaRepository practicas, PrestadorRepository prestadores,
                             ValorConvenidoRepository valores, AuditoriaService auditoria) {
        this.cobertura = cobertura;
        this.practicas = practicas;
        this.prestadores = prestadores;
        this.valores = valores;
        this.auditoria = auditoria;
    }

    @Transactional
    public ResultadoImportacion importar(Long prestadorId, LocalDate vigenteDesde, MultipartFile archivo) {
        prestadores.findById(prestadorId).orElseThrow(() -> new RecursoNoEncontradoException("Prestador", prestadorId));
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Seleccione un archivo.");
        }
        String nombre = archivo.getOriginalFilename() == null ? "archivo" : archivo.getOriginalFilename();
        List<List<String>> filas;
        try (InputStream in = archivo.getInputStream()) {
            String n = nombre.toLowerCase(Locale.ROOT);
            filas = (n.endsWith(".xlsx") || n.endsWith(".xls")) ? leerExcel(in) : leerCsv(in);
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer el archivo: " + e.getMessage());
        }

        int filaEncabezado = -1;
        Map<String, Integer> columnas = Map.of();
        for (int i = 0; i < Math.min(filas.size(), 30) && filaEncabezado < 0; i++) {
            Map<String, Integer> c = detectarColumnas(filas.get(i));
            if (c.containsKey("cmm") && c.containsKey("valor")) {
                filaEncabezado = i;
                columnas = c;
            }
        }
        if (filaEncabezado < 0) {
            throw new ReglaNegocioException("No se encontró la fila de encabezados. Se necesitan al menos las columnas "
                    + "'Código CMM' (o 'CMM') y 'Valor' (o 'Importe').");
        }

        int leidas = 0, creados = 0, actualizados = 0, practicasCreadas = 0;
        List<String> advertencias = new ArrayList<>();
        for (int i = filaEncabezado + 1; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            String codigo = celda(fila, columnas.get("cmm"));
            String valorTxt = celda(fila, columnas.get("valor"));
            if (codigo.isBlank() && valorTxt.isBlank()) {
                continue; // fila vacía o subtítulo (ej. "ENFERMERIA")
            }
            leidas++;
            if (codigo.isBlank()) {
                continue; // filas de título sin código
            }
            codigo = codigo.replaceAll("\\.0+$", "").trim();
            BigDecimal importe = parsearImporte(valorTxt);
            if (importe == null || importe.signum() <= 0) {
                advertencias.add("Fila " + (i + 1) + ": valor inválido para " + codigo + " ('" + valorTxt + "')");
                continue;
            }
            Practica practica = practicas.findByCodigoCmm(codigo).orElse(null);
            if (practica == null) {
                String desc = celda(fila, columnas.get("descripcion"));
                if (desc.isBlank()) {
                    advertencias.add("Fila " + (i + 1) + ": la práctica " + codigo + " no existe y no tiene descripción");
                    continue;
                }
                practica = new Practica();
                practica.setCodigoCmm(codigo);
                practica.setCodigoNomenclador(blankToNull(celda(fila, columnas.get("nomenclador"))));
                practica.setDescripcion(desc.trim());
                String rubro = celda(fila, columnas.get("rubro"));
                practica.setRubro(rubro.isBlank() ? "OTROS" : rubro.trim().toUpperCase());
                practica = practicas.save(practica);
                practicasCreadas++;
            }
            boolean existia = valores.findByPrestadorIdAndPracticaIdAndVigenteDesde(prestadorId, practica.getId(), vigenteDesde).isPresent();
            cobertura.guardarValor(prestadorId, new ValorRequest(practica.getId(), vigenteDesde, importe), "Importación " + nombre);
            if (existia) {
                actualizados++;
            } else {
                creados++;
            }
        }
        auditoria.registrar("prestador", prestadorId, "IMPORTACION_VALORES",
                nombre + ": " + creados + " nuevos, " + actualizados + " actualizados, vigencia " + vigenteDesde);
        return new ResultadoImportacion(leidas, creados, actualizados, practicasCreadas, advertencias);
    }

    // ------------------------------------------------------------------ lectura

    private List<List<String>> leerExcel(InputStream in) throws IOException {
        List<List<String>> filas = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet hoja = wb.getSheetAt(0);
            for (Row row : hoja) {
                List<String> fila = new ArrayList<>();
                int ultima = Math.max(row.getLastCellNum(), 0);
                for (int c = 0; c < ultima; c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) {
                        fila.add("");
                    } else if (cell.getCellType() == CellType.NUMERIC
                            || (cell.getCellType() == CellType.FORMULA && cell.getCachedFormulaResultType() == CellType.NUMERIC)) {
                        // valor numérico crudo para no depender del formato regional de la celda
                        fila.add(BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString());
                    } else {
                        fila.add(formatter.formatCellValue(cell));
                    }
                }
                filas.add(fila);
            }
        }
        return filas;
    }

    private List<List<String>> leerCsv(InputStream in) throws IOException {
        List<List<String>> filas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linea;
            Character sep = null;
            while ((linea = br.readLine()) != null) {
                if (linea.startsWith("﻿")) {
                    linea = linea.substring(1);
                }
                if (sep == null && !linea.isBlank()) {
                    sep = linea.chars().filter(ch -> ch == ';').count() >= linea.chars().filter(ch -> ch == ',').count() ? ';' : ',';
                }
                List<String> fila = new ArrayList<>();
                for (String parte : linea.split(java.util.regex.Pattern.quote(String.valueOf(sep == null ? ';' : sep)), -1)) {
                    fila.add(parte.replaceAll("^\"|\"$", "").trim());
                }
                filas.add(fila);
            }
        }
        return filas;
    }

    static Map<String, Integer> detectarColumnas(List<String> fila) {
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

    static BigDecimal parsearImporte(String texto) {
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

    private static String celda(List<String> fila, Integer idx) {
        if (idx == null || idx >= fila.size()) {
            return "";
        }
        String v = fila.get(idx);
        return v == null ? "" : v.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
