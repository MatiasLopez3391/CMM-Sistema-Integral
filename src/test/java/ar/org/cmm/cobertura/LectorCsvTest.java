package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.importacion.LectorCsv;
import ar.org.cmm.cobertura.importacion.LectorExcel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LectorCsvTest {

    @Test
    void leeCsvConPuntoYComaYSacaLasComillas() throws IOException {
        String csv = "\uFEFFCMM;Detalle;Valor\n\"420101\";\"Consulta\";\"22.911,24\"\n";
        List<List<String>> filas = new LectorCsv().leer(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        assertThat(filas).hasSize(2);
        assertThat(filas.get(0)).containsExactly("CMM", "Detalle", "Valor");
        assertThat(filas.get(1)).containsExactly("420101", "Consulta", "22.911,24");
    }

    @Test
    void elExcelSoloTomaArchivosXlsxOXls() {
        assertThat(new LectorExcel().soporta("planilla.XLSX")).isTrue();
        assertThat(new LectorExcel().soporta("planilla.xls")).isTrue();
        assertThat(new LectorExcel().soporta("planilla.csv")).isFalse();
        assertThat(new LectorCsv().soporta("lo-que-sea.txt")).isTrue();
    }
}
