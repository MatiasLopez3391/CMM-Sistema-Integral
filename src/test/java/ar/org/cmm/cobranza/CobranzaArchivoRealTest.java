package ar.org.cmm.cobranza;

import ar.org.cmm.cobranza.CobranzaDtos.LoteDto;
import ar.org.cmm.cobranza.CobranzaDtos.MedioDto;
import ar.org.cmm.cobranza.CobranzaDtos.ResultadoConfirmacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static ar.org.cmm.cobranza.ArchivoPmcPrueba.pago;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F4: archivo real de PagoMisCuentas (diseño de 100 caracteres con pie de control) y permisos de cobranza. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CobranzaArchivoRealTest {

    @Autowired CobranzaService cobranza;
    @Autowired MockMvc mvc;

    private Long idPmc() {
        return cobranza.medios().stream().filter(m -> m.codigo().equals("PMC")).map(MedioDto::id).findFirst().orElseThrow();
    }

    private static MockMultipartFile archivo(String contenido) {
        return new MockMultipartFile("archivo", "COB4104.test", "text/plain", contenido.getBytes(StandardCharsets.UTF_8));
    }

    private MockHttpSession ingresar(String usuario) throws Exception {
        var res = mvc.perform(post("/api/auth/login").param("usuario", usuario).param("clave", usuario + "2026").with(csrf()))
                .andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    @Test
    void elDisenoDePmcEsElRealYNoElProvisorio() {
        MedioDto pmc = cobranza.medios().stream().filter(m -> m.codigo().equals("PMC")).findFirst().orElseThrow();
        assertThat(pmc.layoutProvisorio()).isFalse();
        assertThat(pmc.layoutNombre()).isEqualTo("PagoMisCuentas - archivo COB");
    }

    @Test
    void importaElArchivoRealVerificaElPieRechazaInexistentesEImputa() {
        String txt = ArchivoPmcPrueba.archivo(
                pago("131090000200", "357001", "20260909", "1500.00"),
                pago("131090000300", "357002", "20260909", "2750.50"),
                pago("999999999999", "357003", "20260909", "900.00"));
        LoteDto lote = cobranza.previsualizar(idPmc(), archivo(txt));
        assertThat(lote.pieControlado()).isTrue();
        assertThat(lote.cantidadRegistros()).isEqualTo(3);
        assertThat(lote.cantidadOk()).isEqualTo(2);
        assertThat(lote.cantidadRechazados()).isEqualTo(1);
        assertThat(lote.importeOk()).isEqualByComparingTo("4250.50");
        assertThat(lote.pagos()).extracting(p -> p.referencia()).containsExactly("357001", "357002", "357003");
        assertThat(lote.pagos().get(2).motivoRechazo()).isEqualTo("Afiliado inexistente");

        ResultadoConfirmacion r = cobranza.confirmar(lote.id());
        assertThat(r.pagosImputados()).isEqualTo(2);
        assertThat(r.importeImputado()).isEqualByComparingTo("4250.50");

        // el mismo archivo, otra vez
        assertThatThrownBy(() -> cobranza.previsualizar(idPmc(), archivo(txt))).hasMessageContaining("ya fue importado");
        // otro archivo que repite una referencia ya imputada
        LoteDto repetido = cobranza.previsualizar(idPmc(), archivo(ArchivoPmcPrueba.archivo(
                pago("131090000200", "357001", "20260909", "1500.00"))));
        assertThat(repetido.cantidadOk()).isZero();
        assertThat(repetido.pagos().get(0).motivoRechazo()).contains("ya registrado");
        cobranza.descartar(repetido.id());
    }

    @Test
    void rechazaElArchivoSiElPieNoCoincide() {
        var p = pago("131090000200", "357010", "20260909", "100.00");
        assertThatThrownBy(() -> cobranza.previsualizar(idPmc(), archivo(ArchivoPmcPrueba.archivoConPie(2, "100.00", p))))
                .hasMessageContaining("El pie del archivo no coincide");
        assertThatThrownBy(() -> cobranza.previsualizar(idPmc(), archivo(ArchivoPmcPrueba.archivoConPie(1, "100.01", p))))
                .hasMessageContaining("El pie del archivo no coincide");
    }

    @Test
    void rechazaElArchivoCortadoSinPie() {
        var p = pago("131090000200", "357020", "20260909", "100.00");
        assertThatThrownBy(() -> cobranza.previsualizar(idPmc(), archivo(ArchivoPmcPrueba.archivoSinPie(p))))
                .hasMessageContaining("no trae un registro de pie");
    }

    @Test
    void soloCobranzaVeLosArchivosYTodosVenLosMedios() throws Exception {
        mvc.perform(get("/api/cobranza/lotes")).andExpect(status().isUnauthorized());
        MockHttpSession gisela = ingresar("gisela");      // Facturación y Cobranza
        MockHttpSession daniela = ingresar("daniela");    // Mesa de entradas
        MockHttpSession ludovico = ingresar("ludovico");  // Médico auditor
        mvc.perform(get("/api/cobranza/lotes").session(gisela)).andExpect(status().isOk());
        mvc.perform(get("/api/cobranza/caja").session(gisela)).andExpect(status().isOk());
        mvc.perform(get("/api/cobranza/lotes").session(daniela)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cobranza/caja").session(ludovico)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cobranza/layouts").session(daniela)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cobranza/medios").session(ludovico)).andExpect(status().isOk());
    }

    @Test
    void previsualizarPorApiExigeSesionCsrfYPermiso() throws Exception {
        String txt = ArchivoPmcPrueba.archivo(pago("131090000300", "357030", "20260909", "300.00"));
        String medio = String.valueOf(idPmc());
        MockHttpSession gisela = ingresar("gisela");
        MockHttpSession daniela = ingresar("daniela");
        mvc.perform(multipart("/api/cobranza/lotes/previsualizar").file(archivo(txt)).param("medioId", medio)
                .session(gisela)).andExpect(status().isForbidden()); // sin token CSRF
        mvc.perform(multipart("/api/cobranza/lotes/previsualizar").file(archivo(txt)).param("medioId", medio)
                .session(daniela).with(csrf())).andExpect(status().isForbidden()); // sin rol
        mvc.perform(multipart("/api/cobranza/lotes/previsualizar").file(archivo(txt)).param("medioId", medio)
                .session(gisela).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pieControlado").value(true))
                .andExpect(jsonPath("$.cantidadOk").value(1));
    }
}
