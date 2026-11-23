package ar.org.cmm.cobertura;

import ar.org.cmm.cobertura.importacion.DetectorColumnas;
import ar.org.cmm.cobertura.importacion.ParserImporte;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImportadorValoresTest {

    @Test
    void detectaColumnasDeLaPlanillaDelPrestadorYTomaElValorMasNuevo() {
        Map<String, Integer> c = DetectorColumnas.detectar(List.of(
                "CMM", "Código", "Detalle Práctica", "VALORES JULIO 2026", "VALORES AGOSTO 2026", "A CARGO DE CIMESA", "A CARGO DEL AFILIADO"));
        assertThat(c).containsEntry("cmm", 0).containsEntry("nomenclador", 1).containsEntry("descripcion", 2).containsEntry("valor", 4);
    }

    @Test
    void interpretaImportesEnFormatoArgentinoYEstadounidense() {
        assertThat(ParserImporte.parsear("$28.118,34")).isEqualByComparingTo("28118.34");
        assertThat(ParserImporte.parsear("$28,118.34")).isEqualByComparingTo("28118.34");
        assertThat(ParserImporte.parsear("22911.24")).isEqualByComparingTo("22911.24");
        assertThat(ParserImporte.parsear("ENFERMERIA")).isNull();
    }
}
