package ar.org.cmm.comun.reportes;

import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.comun.UsuarioActualProvider;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Convierte un {@link Reporte} en PDF (para imprimir) o en Excel (para trabajar los datos). */
@Component
public class ExportadorReportes {

    public static final String INSTITUCION = "Círculo Médico de Mendoza";
    private static final Locale AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color AZUL = new Color(0x16, 0x3A, 0x5F);
    private static final Color GRIS = new Color(0xF0, 0xF3, 0xF7);
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final UsuarioActualProvider usuario;

    public ExportadorReportes(UsuarioActualProvider usuario) {
        this.usuario = usuario;
    }

    /** Respuesta de descarga: formato "pdf" o "xlsx"; nombreBase sin extensión. */
    public ResponseEntity<byte[]> descargar(Reporte r, String formato, String nombreBase) {
        String f = formato == null ? "pdf" : formato.trim().toLowerCase();
        return switch (f) {
            case "pdf" -> respuesta(pdf(r), nombreBase + ".pdf", MediaType.APPLICATION_PDF);
            case "xlsx", "excel" -> respuesta(excel(r), nombreBase + ".xlsx", XLSX);
            default -> throw new ReglaNegocioException("Formato no soportado: " + formato + " (use pdf o xlsx).");
        };
    }

    private static ResponseEntity<byte[]> respuesta(byte[] contenido, String nombre, MediaType tipo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(tipo)
                .body(contenido);
    }

    // ------------------------------------------------------------------ PDF

    public byte[] pdf(Reporte r) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean ancha = r.tablas().stream().anyMatch(t -> t.columnas().size() > 6);
        Rectangle tam = ancha ? PageSize.A4.rotate() : PageSize.A4;
        Document doc = new Document(tam, 32, 32, 36, 40);
        try {
            PdfWriter.getInstance(doc, out);
            Font chica = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.GRAY);
            HeaderFooter pie = new HeaderFooter(new Phrase(INSTITUCION + " · Sistema Integral de Gestión · Página ", chica), true);
            pie.setAlignment(Element.ALIGN_RIGHT);
            pie.setBorder(Rectangle.TOP);
            pie.setBorderColor(Color.LIGHT_GRAY);
            doc.setFooter(pie);
            doc.open();

            doc.add(new Paragraph(INSTITUCION.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, AZUL)));
            Paragraph titulo = new Paragraph(r.titulo(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.BLACK));
            titulo.setSpacingAfter(4);
            doc.add(titulo);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            for (String d : r.datos()) {
                doc.add(new Paragraph(d, normal));
            }
            Paragraph emitido = new Paragraph("Emitido el " + LocalDateTime.now().format(FECHA_HORA) + " por " + usuario.usuario(), chica);
            emitido.setSpacingAfter(10);
            doc.add(emitido);

            for (Reporte.Tabla tabla : r.tablas()) {
                if (tabla.subtitulo() != null) {
                    Paragraph st = new Paragraph(tabla.subtitulo(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, AZUL));
                    st.setSpacingBefore(8);
                    st.setSpacingAfter(4);
                    doc.add(st);
                }
                if (tabla.filas().isEmpty()) {
                    doc.add(new Paragraph("Sin datos para los filtros elegidos.", normal));
                    continue;
                }
                doc.add(tablaPdf(tabla));
            }
            for (String nota : r.notas()) {
                Paragraph p = new Paragraph(nota, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.DARK_GRAY));
                p.setSpacingBefore(6);
                doc.add(p);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    private static PdfPTable tablaPdf(Reporte.Tabla tabla) throws com.lowagie.text.DocumentException {
        List<Reporte.Columna> cols = tabla.columnas();
        PdfPTable t = new PdfPTable(cols.size());
        t.setWidthPercentage(100);
        float[] anchos = new float[cols.size()];
        for (int i = 0; i < anchos.length; i++) {
            anchos[i] = cols.get(i).ancho();
        }
        t.setWidths(anchos);
        t.setHeaderRows(1);
        Font cab = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (Reporte.Columna c : cols) {
            PdfPCell celda = new PdfPCell(new Phrase(c.titulo(), cab));
            celda.setBackgroundColor(AZUL);
            celda.setPadding(4);
            celda.setHorizontalAlignment(alineacion(c.tipo()));
            t.addCell(celda);
        }
        Font dato = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        int n = 0;
        for (List<Object> fila : tabla.filas()) {
            agregarFila(t, cols, fila, dato, n++ % 2 == 1 ? GRIS : null);
        }
        if (tabla.totales() != null) {
            agregarFila(t, cols, tabla.totales(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK), new Color(0xE3, 0xEA, 0xF3));
        }
        return t;
    }

    private static void agregarFila(PdfPTable t, List<Reporte.Columna> cols, List<Object> fila, Font f, Color fondo) {
        for (int i = 0; i < cols.size(); i++) {
            Object v = i < fila.size() ? fila.get(i) : null;
            PdfPCell celda = new PdfPCell(new Phrase(texto(v, cols.get(i).tipo()), f));
            celda.setPadding(3.5f);
            celda.setBorderColor(Color.LIGHT_GRAY);
            celda.setHorizontalAlignment(alineacion(cols.get(i).tipo()));
            if (fondo != null) {
                celda.setBackgroundColor(fondo);
            }
            t.addCell(celda);
        }
    }

    private static int alineacion(Reporte.Tipo tipo) {
        return switch (tipo) {
            case MONEDA, NUMERO, PORCENTAJE -> Element.ALIGN_RIGHT;
            case FECHA -> Element.ALIGN_CENTER;
            default -> Element.ALIGN_LEFT;
        };
    }

    /** Texto de una celda con el formato argentino ($ 1.234,56; 21/11/2026; 4,2 %). */
    static String texto(Object v, Reporte.Tipo tipo) {
        if (v == null) {
            return "";
        }
        return switch (tipo) {
            case MONEDA -> v instanceof Number n ? NumberFormat.getCurrencyInstance(AR).format(n) : v.toString();
            case NUMERO -> v instanceof Number n ? NumberFormat.getNumberInstance(AR).format(n) : v.toString();
            case PORCENTAJE -> v instanceof Number n ? NumberFormat.getNumberInstance(AR).format(n) + " %" : v.toString();
            case FECHA -> v instanceof LocalDate d ? d.format(FECHA) : v.toString();
            default -> v.toString();
        };
    }

    // ------------------------------------------------------------------ Excel

    public byte[] excel(Reporte r) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String nombreHoja = r.titulo().replaceAll("[\\\\/?*\\[\\]:]", " ");
            Sheet hoja = wb.createSheet(nombreHoja.length() > 31 ? nombreHoja.substring(0, 31) : nombreHoja);
            org.apache.poi.ss.usermodel.Font negrita = wb.createFont();
            negrita.setBold(true);
            CellStyle sTitulo = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font fTitulo = wb.createFont();
            fTitulo.setBold(true);
            fTitulo.setFontHeightInPoints((short) 14);
            sTitulo.setFont(fTitulo);
            CellStyle sCab = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font fCab = wb.createFont();
            fCab.setBold(true);
            fCab.setColor(IndexedColors.WHITE.getIndex());
            sCab.setFont(fCab);
            sCab.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            sCab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            short fmtMoneda = wb.createDataFormat().getFormat("\"$\" #,##0.00");
            short fmtNumero = wb.createDataFormat().getFormat("#,##0.##");
            short fmtFecha = wb.createDataFormat().getFormat("dd/mm/yyyy");
            CellStyle sMoneda = estilo(wb, fmtMoneda, false, negrita);
            CellStyle sNumero = estilo(wb, fmtNumero, false, negrita);
            CellStyle sFecha = estilo(wb, fmtFecha, false, negrita);
            CellStyle sMonedaTot = estilo(wb, fmtMoneda, true, negrita);
            CellStyle sNumeroTot = estilo(wb, fmtNumero, true, negrita);
            CellStyle sTextoTot = wb.createCellStyle();
            sTextoTot.setFont(negrita);
            sTextoTot.setBorderTop(BorderStyle.THIN);

            int f = 0;
            Row rt = hoja.createRow(f++);
            rt.createCell(0).setCellValue(INSTITUCION + " · " + r.titulo());
            rt.getCell(0).setCellStyle(sTitulo);
            for (String d : r.datos()) {
                hoja.createRow(f++).createCell(0).setCellValue(d);
            }
            hoja.createRow(f++).createCell(0).setCellValue("Emitido el " + LocalDateTime.now().format(FECHA_HORA) + " por " + usuario.usuario());
            f++;
            CellStyle sSub = wb.createCellStyle();
            sSub.setFont(negrita);
            int maxCols = 1;
            for (Reporte.Tabla tabla : r.tablas()) {
                if (tabla.subtitulo() != null) {
                    Row rs = hoja.createRow(f++);
                    rs.createCell(0).setCellValue(tabla.subtitulo());
                    rs.getCell(0).setCellStyle(sSub);
                }
                Row cab = hoja.createRow(f++);
                for (int i = 0; i < tabla.columnas().size(); i++) {
                    Cell c = cab.createCell(i);
                    c.setCellValue(tabla.columnas().get(i).titulo());
                    c.setCellStyle(sCab);
                }
                for (List<Object> fila : tabla.filas()) {
                    escribirFila(hoja.createRow(f++), tabla.columnas(), fila, sMoneda, sNumero, sFecha, null);
                }
                if (tabla.totales() != null) {
                    escribirFila(hoja.createRow(f++), tabla.columnas(), tabla.totales(), sMonedaTot, sNumeroTot, sFecha, sTextoTot);
                }
                maxCols = Math.max(maxCols, tabla.columnas().size());
                f++;
            }
            for (String nota : r.notas()) {
                f++;
                hoja.createRow(f).createCell(0).setCellValue(nota);
            }
            for (int i = 0; i < maxCols; i++) {
                hoja.autoSizeColumn(i);
                hoja.setColumnWidth(i, Math.min(Math.max(hoja.getColumnWidth(i), 2800), 18000));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el Excel: " + e.getMessage(), e);
        }
    }

    private static CellStyle estilo(XSSFWorkbook wb, short formato, boolean total, org.apache.poi.ss.usermodel.Font negrita) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(formato);
        if (total) {
            s.setFont(negrita);
            s.setBorderTop(BorderStyle.THIN);
        }
        return s;
    }

    private static void escribirFila(Row row, List<Reporte.Columna> cols, List<Object> fila, CellStyle sMoneda, CellStyle sNumero,
                                     CellStyle sFecha, CellStyle sTexto) {
        for (int i = 0; i < cols.size(); i++) {
            Object v = i < fila.size() ? fila.get(i) : null;
            if (v == null) {
                continue;
            }
            Cell c = row.createCell(i);
            switch (cols.get(i).tipo()) {
                case MONEDA, NUMERO, PORCENTAJE -> {
                    if (v instanceof Number n) {
                        c.setCellValue(n instanceof BigDecimal b ? b.doubleValue() : n.doubleValue());
                        c.setCellStyle(cols.get(i).tipo() == Reporte.Tipo.MONEDA ? sMoneda : sNumero);
                    } else {
                        c.setCellValue(v.toString());
                        if (sTexto != null) {
                            c.setCellStyle(sTexto);
                        }
                    }
                }
                case FECHA -> {
                    if (v instanceof LocalDate d) {
                        c.setCellValue(d);
                        c.setCellStyle(sFecha);
                    } else {
                        c.setCellValue(v.toString());
                    }
                }
                default -> {
                    c.setCellValue(v.toString());
                    if (sTexto != null) {
                        c.setCellStyle(sTexto);
                    }
                }
            }
        }
    }
}
