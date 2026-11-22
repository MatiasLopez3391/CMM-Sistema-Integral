package ar.org.cmm.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login, sesión, CSRF y roles (T36), usando los usuarios de demostración de db/demo/V952. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadTest {

    @Autowired MockMvc mvc;

    private MockHttpSession ingresar(String usuario, String clave) throws Exception {
        var res = mvc.perform(post("/api/auth/login").param("usuario", usuario).param("clave", clave).with(csrf()))
                .andExpect(status().isNoContent())
                .andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    @Test
    void sinSesionLaApiDaNoAutorizado() throws Exception {
        mvc.perform(get("/api/afiliados")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/yo")).andExpect(status().isUnauthorized());
    }

    @Test
    void laPantallaDeIngresoEsPublica() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }

    @Test
    void claveIncorrectaEsRechazada() throws Exception {
        mvc.perform(post("/api/auth/login").param("usuario", "daniela").param("clave", "mala").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos."));
    }

    @Test
    void usuarioInexistenteEsRechazado() throws Exception {
        mvc.perform(post("/api/auth/login").param("usuario", "nadie").param("clave", "nadie2026").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ingresoSinTokenCsrfEsRechazado() throws Exception {
        mvc.perform(post("/api/auth/login").param("usuario", "daniela").param("clave", "daniela2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ingresoCorrectoDevuelveNombreYRoles() throws Exception {
        MockHttpSession sesion = ingresar("daniela", "daniela2026");
        mvc.perform(get("/api/auth/yo").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("daniela"))
                .andExpect(jsonPath("$.nombre").value("Daniela (Afiliaciones)"))
                .andExpect(jsonPath("$.roles[0]").value("ADMINISTRACION"));
    }

    @Test
    void elUsuarioEsInsensibleAMayusculasPeroLaClaveNo() throws Exception {
        ingresar("Gisela", "gisela2026");
        mvc.perform(post("/api/auth/login").param("usuario", "gisela").param("clave", "GISELA2026").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void losRolesDeLasAnotacionesSeHacenCumplir() throws Exception {
        MockHttpSession auditor = ingresar("ludovico", "ludovico2026");
        // el auditor no es de Facturación
        mvc.perform(get("/api/facturacion/periodos").session(auditor)).andExpect(status().isForbidden());

        MockHttpSession liquidaciones = ingresar("gisela", "gisela2026");
        mvc.perform(get("/api/facturacion/periodos").session(liquidaciones)).andExpect(status().isOk());
        // Liquidaciones no ve la historia clínica; el auditor sí
        mvc.perform(get("/api/afiliados/1/historia-clinica").session(liquidaciones)).andExpect(status().isForbidden());
        mvc.perform(get("/api/afiliados/1/historia-clinica").session(auditor))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void cerrarSesionInvalidaLaSesion() throws Exception {
        MockHttpSession sesion = ingresar("silvina", "silvina2026");
        mvc.perform(post("/api/auth/logout").session(sesion).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/yo").session(sesion)).andExpect(status().isUnauthorized());
    }
}
