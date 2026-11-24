
package ar.org.cmm.ordenes;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InformeCosegurosSeguridadTest {

    private static final String URL =
            "/api/ordenes/informe-coseguros";

    @Autowired
    private MockMvc mvc;

    private MockHttpSession ingresar(
            String usuario,
            String clave) throws Exception {

        var resultado = mvc.perform(
                post("/api/auth/login")
                        .param("usuario", usuario)
                        .param("clave", clave)
                        .with(csrf())
        )
                .andExpect(status().isNoContent())
                .andReturn();

        return (MockHttpSession) resultado
                .getRequest()
                .getSession(false);
    }

    @Test
    void sinSesionNoPuedeConsultarInforme() throws Exception {

        mvc.perform(
                get(URL)
                        .param("desde", "2026-11-01")
                        .param("hasta", "2026-11-30")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void mesaEntradasNoPuedeConsultarInforme() throws Exception {

        MockHttpSession sesion =
                ingresar("daniela", "daniela2026");

        mvc.perform(
                get(URL)
                        .session(sesion)
                        .param("desde", "2026-11-01")
                        .param("hasta", "2026-11-30")
        ).andExpect(status().isForbidden());
    }

    @Test
    void medicoAuditorNoPuedeConsultarInforme() throws Exception {

        MockHttpSession sesion =
                ingresar("ludovico", "ludovico2026");

        mvc.perform(
                get(URL)
                        .session(sesion)
                        .param("desde", "2026-11-01")
                        .param("hasta", "2026-11-30")
        ).andExpect(status().isForbidden());
    }

    @Test
    void liquidacionesPuedeConsultarInforme() throws Exception {

        MockHttpSession sesion =
                ingresar("gisela", "gisela2026");

        mvc.perform(
                get(URL)
                        .session(sesion)
                        .param("desde", "2026-11-01")
                        .param("hasta", "2026-11-30")
        ).andExpect(status().isOk());
    }

    @Test
    void usuarioConTodosLosRolesPuedeConsultarInforme()
            throws Exception {

        MockHttpSession sesion =
                ingresar("silvina", "silvina2026");

        mvc.perform(
                get(URL)
                        .session(sesion)
                        .param("desde", "2026-11-01")
                        .param("hasta", "2026-11-30")
        ).andExpect(status().isOk());
    }
}
