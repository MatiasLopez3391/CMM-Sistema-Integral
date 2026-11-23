package ar.org.cmm.comun;

import ar.org.cmm.comun.reportes.ExportadorReportes;
import ar.org.cmm.comun.reportes.Reporte;
import ar.org.cmm.comun.reportes.Reporte.Tipo;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** PDF y Excel de los reportes imprimibles (T47), sin base ni servidor. */
class ExportadorReportesTest {

    private final ExportadorReportes exportador = new ExportadorReportes(new UsuarioActualProvider() {
        @Override
        public String usuario() {
            return "gisela";
        }

        @Override
        public Set<Rol> roles() {
            return Set.of(Rol.LIQUIDACIONES);
        }
    });

    private final Reporte reporte = Reporte.de("Estado de cuenta")
            .dato("Titular", "131090000100 · GÓMEZ, Laura Beatriz")
            .tabla("Movimientos")
            .columna("Fecha", Tipo.FECHA, 1).columna("Concepto", Tipo.TEXTO, 4).columna("Debe", Tipo.MONEDA, 1.5f)
            .fila(LocalDate.of(2026, 11, 1), "Cuota 11/2026", new BigDecimal("723517.65"))
            .fila(LocalDate.of(2026, 11, 9), "Pago PagoMisCuentas", null)
            .totales(null, "Totales", new BigDecimal("723517.65"))
            .tabla("Comprobantes")
            .columna("Número", Tipo.TEXTO, 1).columna("Saldo", Tipo.MONEDA, 1)
            .fila("CI-00000023", BigDecimal.ZERO)
            .nota("Comprobantes internos, no válidos como factura.")
            .build();

    @Test
    void generaUnPdf() {
        byte[] pdf = exportador.pdf(reporte);
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1000);
    }

    @Test
    void generaUnExcelConLosValoresComoNumeros() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(exportador.excel(reporte)))) {
            Sheet hoja = wb.getSheetAt(0);
            assertThat(hoja.getRow(0).getCell(0).getStringCellValue()).contains("Estado de cuenta");
            Row cuota = null;
            for (Row r : hoja) {
                if (r.getCell(1) != null && r.getCell(1).getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                        && "Cuota 11/2026".equals(r.getCell(1).getStringCellValue())) {
                    cuota = r;
                }
            }
            assertThat(cuota).isNotNull();
            assertThat(cuota.getCell(2).getNumericCellValue()).isEqualTo(723517.65);
            assertThat(cuota.getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        }
    }
}
