package ar.org.cmm.cobertura;

import ar.org.cmm.comun.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RubroTest {

    @Test
    void normalizaMayusculasTildesYEspacios() {
        assertThat(Rubro.normalizar(" Enfermería ")).isEqualTo("ENFERMERIA");
        assertThat(Rubro.normalizar("medicamentos")).isEqualTo("MEDICAMENTOS");
    }

    @Test
    void rechazaRubrosDesconocidosOVacios() {
        assertThatThrownBy(() -> Rubro.normalizar("CIRUGIA")).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Rubro inválido");
        assertThatThrownBy(() -> Rubro.normalizar("  ")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> Rubro.normalizar(null)).isInstanceOf(ReglaNegocioException.class);
    }
}
