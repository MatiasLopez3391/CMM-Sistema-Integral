package ar.org.cmm.cobranza;

import ar.org.cmm.cobranza.LectorArchivoCobro.RegistroLeido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LectorArchivoCobroTest {

    private final LectorArchivoCobro lector = new LectorArchivoCobro();

    @Test
    void leeAnchoFijoConDecimalesImplicitosEIgnoraEncabezadoYPie() {
        LayoutArchivo l = new LayoutArchivo();
        l.setFormato(LayoutArchivo.ANCHO_FIJO);
        l.setPrefijoRegistro("D");
        l.setAfiliadoPos(2);
        l.setAfiliadoLargo(12);
        l.setFechaPos(14);
        l.setFechaLargo(8);
        l.setFechaFormato("yyyyMMdd");
        l.setImportePos(22);
        l.setImporteLargo(12);
        l.setImporteDecimalesImpl(2);
        l.setReferenciaPos(34);
        l.setReferenciaLargo(15);
        String archivo = """
                HCMM-CIMESA          20260909RENDICION
                D13109000010020260909000069260400PMC202609000001
                D1310900002002026XX09000018240400PMC202609000002
                T000002000000874844
                """;
        List<RegistroLeido> r = lector.leer(archivo, l);
        assertThat(r).hasSize(2);
        assertThat(r.get(0).valido()).isTrue();
        assertThat(r.get(0).nroAfiliado()).isEqualTo("131090000100");
        assertThat(r.get(0).fecha()).isEqualTo(LocalDate.of(2026, 9, 9));
        assertThat(r.get(0).importe()).isEqualByComparingTo("692604.00");
        assertThat(r.get(0).referencia()).isEqualTo("PMC202609000001");
        assertThat(r.get(1).valido()).isFalse();
        assertThat(r.get(1).error()).isEqualTo("Fecha inválida");
    }

    @Test
    void leeDelimitadoConComaDecimal() {
        LayoutArchivo l = new LayoutArchivo();
        l.setFormato(LayoutArchivo.DELIMITADO);
        l.setDelimitador(";");
        l.setLineasEncabezado(1);
        l.setAfiliadoPos(1);
        l.setFechaPos(2);
        l.setFechaFormato("dd/MM/yyyy");
        l.setImportePos(3);
        l.setReferenciaPos(4);
        List<RegistroLeido> r = lector.leer("nro;fecha;importe;ref\n131090000600;09/09/2026;182404,00;RP-1\n", l);
        assertThat(r).singleElement().satisfies(x -> {
            assertThat(x.importe()).isEqualByComparingTo(new BigDecimal("182404.00"));
            assertThat(x.referencia()).isEqualTo("RP-1");
        });
    }
}
