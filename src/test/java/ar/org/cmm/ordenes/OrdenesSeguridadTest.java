package ar.org.cmm.ordenes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quién puede emitir, consultar, anular y auditar órdenes, y que los errores de negocio lleguen como 422. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrdenesSeguridadTest {

    @Autowired
    private MockMvc mvc;

    private MockHttpSession ingresar(String usuario) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").param("usuario", usuario)
                        .param("clave", usuario + "2026").with(csrf()))
                .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
    }

    @Test
    void mesaDeEntradasEmiteConsultaYRecibeAvisos() throws Exception {
        MockHttpSession s = ingresar("daniela");
        mvc.perform(get("/api/ordenes").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/ordenes/avisos").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/auditoria-medica/pendientes").session(s)).andExpect(status().isForbidden());
    }

    @Test
    void elAuditorConsultaYDecidePeroNoEmite() throws Exception {
        MockHttpSession s = ingresar("ludovico");
        mvc.perform(get("/api/auditoria-medica/pendientes").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/auditoria-medica/resueltas").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/ordenes").session(s)).andExpect(status().isOk());
        mvc.perform(post("/api/ordenes").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"afiliadoId\":1,\"prestadorId\":1,\"items\":[{\"practicaId\":1,\"cantidad\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void liquidacionesConsultaPeroNoAnula() throws Exception {
        MockHttpSession s = ingresar("gisela");
        mvc.perform(get("/api/ordenes").session(s)).andExpect(status().isOk());
        mvc.perform(post("/api/ordenes/1/anular").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
    }

    @Test
    void unErrorDeNegocioSeMuestraAlOperador() throws Exception {
        MockHttpSession s = ingresar("daniela");
        mvc.perform(post("/api/ordenes/999999/anular").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/ordenes/simular").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"afiliadoId\":1,\"prestadorId\":1,\"items\":[{\"practicaId\":1,\"cantidad\":1},{\"practicaId\":1,\"cantidad\":2}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("repetir")));
    }
}
