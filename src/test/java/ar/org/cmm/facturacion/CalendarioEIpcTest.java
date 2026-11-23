package ar.org.cmm.facturacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas puras de T44 y T45: día hábil siguiente y lectura de las APIs públicas (sin red ni base). */
class CalendarioEIpcTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void elVencimientoPasaAlDiaHabilSiguiente() {
        Set<LocalDate> feriados = Set.of(LocalDate.of(2026, 10, 12));
        // 10/10/2026 es sábado y el lunes 12 es feriado: vence el martes 13
        assertThat(CalendarioHabilService.habilDesde(LocalDate.of(2026, 10, 10), feriados::contains)).isEqualTo(LocalDate.of(2026, 10, 13));
        // 10/11/2026 es martes hábil: no cambia
        assertThat(CalendarioHabilService.habilDesde(LocalDate.of(2026, 11, 10), feriados::contains)).isEqualTo(LocalDate.of(2026, 11, 10));
        // Domingo sin feriados: pasa al lunes
        assertThat(CalendarioHabilService.habilDesde(LocalDate.of(2027, 1, 10), d -> false)).isEqualTo(LocalDate.of(2027, 1, 11));
    }

    @Test
    void leeLosFeriadosDeLaApi() {
        String cuerpo = """
                [{"fecha":"2027-01-01","tipo":"inamovible","nombre":"Año nuevo"},
                 {"fecha":"2027-02-08","tipo":"inamovible","nombre":"Carnaval"}]""";
        List<Feriado> l = CalendarioHabilService.parsear(cuerpo, json);
        assertThat(l).extracting(Feriado::getFecha).containsExactly(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 8));
        assertThat(l).allSatisfy(f -> assertThat(f.getOrigen()).isEqualTo("API"));
    }

    @Test
    void calculaLaVariacionMensualDesdeElIndiceDelIndec() {
        // La API devuelve el índice de nivel general, del mes más nuevo al más viejo
        String cuerpo = """
                {"data":[["2026-10-01",10450.0],["2026-09-01",10200.0],["2026-08-01",10000.0],["2026-06-01",9700.0]],
                 "count":4,"meta":[]}""";
        Map<String, BigDecimal> v = IpcService.variacionesDesdeSerie(cuerpo, json);
        // ago→sep: 10200/10000 − 1 = 2 %; sep→oct: 10450/10200 − 1 = 2,451 %; jun→ago no son consecutivos
        assertThat(v).containsOnlyKeys("202609", "202610");
        assertThat(v.get("202609")).isEqualByComparingTo("2.000");
        assertThat(v.get("202610")).isEqualByComparingTo("2.451");
    }

    @Test
    void unaRespuestaInesperadaDaUnMensajeClaro() {
        assertThatThrownBy(() -> IpcService.variacionesDesdeSerie("<html>error</html>", json))
                .hasMessageContaining("No se pudo leer la respuesta del IPC");
        assertThatThrownBy(() -> IpcService.variacionesDesdeSerie("{\"data\":[[\"2026-10-01\",10450.0]]}", json))
                .hasMessageContaining("dos meses consecutivos");
    }
}
