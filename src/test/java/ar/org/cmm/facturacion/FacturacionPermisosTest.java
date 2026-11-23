package ar.org.cmm.facturacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Permisos de las pantallas nuevas de F3 por la API (solo consultas, para no alterar los datos de otras pruebas).
 * Liquidaciones (gisela) entra; Mesa de Entradas (daniela) y el médico auditor (ludovico) no.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FacturacionPermisosTest {

    @Autowired MockMvc mvc;

    private MockHttpSession ingresar(String usuario) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").param("usuario", usuario).param("clave", usuario + "2026").with(csrf()))
                .andExpect(status().isNoContent())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void liquidacionesConsultaElHistorialDeCoseguros() throws Exception {
        mvc.perform(get("/api/facturacion/coseguros").param("desde", "202612").param("hasta", "202612").session(ingresar("gisela")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porPeriodo[0].periodo").value("202612"));
    }

    @Test
    void mesaDeEntradasYAuditorNoVenLosCoseguros() throws Exception {
        mvc.perform(get("/api/facturacion/coseguros").param("desde", "202601").param("hasta", "202612").session(ingresar("daniela")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/facturacion/coseguros").param("desde", "202601").param("hasta", "202612").session(ingresar("ludovico")))
                .andExpect(status().isForbidden());
    }

    @Test
    void liquidacionesDescargaElListadoDeDeudoresEnPdfYExcel() throws Exception {
        MockHttpSession gisela = ingresar("gisela");
        mvc.perform(get("/api/facturacion/imprimir/morosos").param("formato", "pdf").param("alDia", "2026-11-22").session(gisela))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("deudores-2026-11-22.pdf")));
        mvc.perform(get("/api/facturacion/imprimir/morosos").param("formato", "xlsx").session(gisela))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")));
        mvc.perform(get("/api/facturacion/imprimir/morosos").session(ingresar("daniela"))).andExpect(status().isForbidden());
    }

    @Test
    void elFormatoDelArchivoContableSeVeConUnEjemplo() throws Exception {
        mvc.perform(get("/api/facturacion/asientos/formato").session(ingresar("gisela")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formato.separador").value(";"))
                .andExpect(jsonPath("$.ejemplo").value(org.hamcrest.Matchers.startsWith("nro_asiento;fecha;cuenta;debe;haber")));
    }
}
