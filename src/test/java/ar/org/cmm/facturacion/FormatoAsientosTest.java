package ar.org.cmm.facturacion;

import ar.org.cmm.facturacion.ExportacionAsientosService.Formato;
import ar.org.cmm.facturacion.ExportacionAsientosService.Linea;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** El archivo de asientos se adapta al sistema contable cambiando solo el formato (T48). */
class FormatoAsientosTest {

    private final List<Linea> lineas = List.of(
            new Linea("6", 1, LocalDate.of(2026, 11, 1), "1.1.03.01", new BigDecimal("1500.5"), BigDecimal.ZERO, "Cuota 11/2026; GÓMEZ", "CI-1"),
            new Linea("6", 1, LocalDate.of(2026, 11, 1), "4.1.01.01", BigDecimal.ZERO, new BigDecimal("1500.5"), "Cuota 11/2026; GÓMEZ", "CI-1"));

    @Test
    void formatoPorDefecto() {
        Formato f = new Formato(List.of("nro_asiento", "fecha", "cuenta", "debe", "haber", "concepto", "referencia"), ";", "dd/MM/yyyy",
                ".", true, "csv");
        assertThat(ExportacionAsientosService.renderizar(lineas, f)).isEqualTo("""
                nro_asiento;fecha;cuenta;debe;haber;concepto;referencia
                1;01/11/2026;1.1.03.01;1500.50;0.00;Cuota 11/2026, GÓMEZ;CI-1
                1;01/11/2026;4.1.01.01;0.00;1500.50;Cuota 11/2026, GÓMEZ;CI-1
                """);
    }

    @Test
    void conLaColumnaLote() {
        Formato f = new Formato(List.of("lote", "nro_asiento", "cuenta", "debe", "haber"), ";", "dd/MM/yyyy", ".", false, "csv");
        assertThat(ExportacionAsientosService.renderizar(lineas, f)).startsWith("6;1;1.1.03.01;1500.50;0.00\n");
    }

    @Test
    void otroSistemaConTabuladorDecimalComaEImporteConIndicadorDH() {
        Formato f = new Formato(List.of("fecha", "cuenta", "importe", "dh", "concepto"), "\t", "yyyyMMdd", ",", false, "txt");
        assertThat(ExportacionAsientosService.renderizar(lineas, f)).isEqualTo(
                "20261101\t1.1.03.01\t1500,50\tD\tCuota 11/2026; GÓMEZ\n"
                        + "20261101\t4.1.01.01\t1500,50\tH\tCuota 11/2026; GÓMEZ\n");
    }
}
