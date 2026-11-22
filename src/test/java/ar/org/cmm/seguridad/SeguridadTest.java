package ar.org.cmm.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login, sesión, CSRF y roles (T36), usando los usuarios de demostración de db/demo/V952. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

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
    void laRaizYLosArchivosDelFrontendSonPublicos() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mvc.perform(get("/js/vistas/afiliados.js")).andExpect(status().isOk());
        mvc.perform(get("/css/app.css")).andExpect(status().isOk());
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
                .andExpect(jsonPath("$.nombre").value("Daniela"))
                .andExpect(jsonPath("$.roles[0]").value("MESA_ENTRADAS"));
    }

    @Test
    void elUsuarioEsInsensibleAMayusculasPeroLaClaveNo() throws Exception {
        ingresar("Gisela", "gisela2026");
        mvc.perform(post("/api/auth/login").param("usuario", "gisela").param("clave", "GISELA2026").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // Los cuerpos vacíos ("{}") no crean nada: con permiso la validación responde 400; sin permiso, 403.
    private static final String VACIO = "{}";

    @Test
    void medicoAuditorSoloConsulta() throws Exception {
        MockHttpSession s = ingresar("ludovico", "ludovico2026");
        mvc.perform(get("/api/auth/yo").session(s)).andExpect(jsonPath("$.nombre").value("Médico Auditor"));
        // puede consultar datos del afiliado, incluida la historia clínica
        mvc.perform(get("/api/afiliados").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/cobertura/planes").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/afiliados/1/historia-clinica").session(s))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
        // no puede editar nada
        mvc.perform(post("/api/afiliados").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
        mvc.perform(put("/api/afiliados/1").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
        mvc.perform(post("/api/afiliados/1/baja").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
        mvc.perform(post("/api/afiliados/1/bloqueo").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
        mvc.perform(post("/api/afiliados/1/historia-clinica").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
        // ni ver facturación ni cobranza
        mvc.perform(get("/api/facturacion/periodos").session(s)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cobranza/caja").session(s)).andExpect(status().isForbidden());
    }

    @Test
    void facturacionYCobranzaEditaAfiliadosYUsaFacturacion() throws Exception {
        MockHttpSession s = ingresar("gisela", "gisela2026");
        mvc.perform(get("/api/facturacion/periodos").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/afiliados").session(s)).andExpect(status().isOk());
        mvc.perform(post("/api/afiliados").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/afiliados/1/bloqueo").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isBadRequest());
        // ve la historia clínica pero no la carga
        mvc.perform(get("/api/afiliados/1/historia-clinica").session(s))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
        mvc.perform(post("/api/afiliados/1/historia-clinica").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
    }

    @Test
    void emisoraDeOrdenesEditaAfiliadosYCargaHistoriaPeroNoVeFacturacion() throws Exception {
        MockHttpSession s = ingresar("daniela", "daniela2026");
        mvc.perform(post("/api/afiliados").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/afiliados/1/historia-clinica").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isBadRequest());
        // lo que necesita el formulario de afiliados
        mvc.perform(get("/api/cobertura/planes").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/cobertura/derivadoras").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/cobranza/medios").session(s)).andExpect(status().isOk());
        // pero no facturación ni cobranza
        mvc.perform(get("/api/facturacion/periodos").session(s)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cobranza/caja").session(s)).andExpect(status().isForbidden());
    }

    @Test
    void permisoTotalTieneTodosLosRoles() throws Exception {
        MockHttpSession s = ingresar("silvina", "silvina2026");
        mvc.perform(get("/api/auth/yo").session(s))
                .andExpect(jsonPath("$.nombre").value("Silvina"))
                .andExpect(jsonPath("$.roles.length()").value(5));
        mvc.perform(get("/api/facturacion/periodos").session(s)).andExpect(status().isOk());
        mvc.perform(post("/api/afiliados").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/afiliados/1/historia-clinica").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isBadRequest());
    }

    @Test
    void cerrarSesionInvalidaLaSesion() throws Exception {
        MockHttpSession sesion = ingresar("silvina", "silvina2026");
        mvc.perform(post("/api/auth/logout").session(sesion).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/yo").session(sesion)).andExpect(status().isUnauthorized());
    }
}
