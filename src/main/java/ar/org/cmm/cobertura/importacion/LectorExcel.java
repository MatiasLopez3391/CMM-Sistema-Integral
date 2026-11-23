package ar.org.cmm.cobertura.importacion;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lee la primera hoja de un Excel (.xlsx o .xls). */
@Component
@Order(1)
public class LectorExcel implements LectorPlanilla {

    @Override
    public boolean soporta(String nombreArchivo) {
        String n = nombreArchivo.toLowerCase(Locale.ROOT);
        return n.endsWith(".xlsx") || n.endsWith(".xls");
    }

    @Override
    public List<List<String>> leer(InputStream in) throws IOException {
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
}
