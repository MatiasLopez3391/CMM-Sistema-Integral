package ar.org.cmm.cobertura;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Permisos de la API de F2 con el login de F1 (usuarios de demostración de db/demo/V952 y V953).
 * Con cuerpo vacío ("{}"): con permiso la validación responde 400; sin permiso, 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoberturaSeguridadTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final String VACIO = "{}";
    private static final String COSEGURO = "?prestadorId=1&practicaId=4&planId=1&fecha=2026-08-15";

    @Autowired MockMvc mvc;

    private MockHttpSession ingresar(String usuario, String clave) throws Exception {
        var res = mvc.perform(post("/api/auth/login").param("usuario", usuario).param("clave", clave).with(csrf()))
                .andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    @Test
    void mesaDeEntradasConsultaLoQueNecesitaParaEmitirOrdenesPeroNoModificaNada() throws Exception {
        MockHttpSession s = ingresar("daniela", "daniela2026");
        mvc.perform(get("/api/cobertura/planes").session(s)).andExpect(status().isOk());
        // este GET recorre el conteo de afiliados por derivadora que ahora publica F1
        mvc.perform(get("/api/cobertura/derivadoras").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/cobertura/prestadores").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/cobertura/practicas").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/cobertura/coseguro" + COSEGURO).session(s)).andExpect(status().isOk())
                .andExpect(jsonPath("$.planCodigo").exists());
        mvc.perform(get("/api/cobertura/coseguro/simulador" + COSEGURO).session(s)).andExpect(status().isOk());
        // consulta los valores convenidos para informar el importe de la orden
        mvc.perform(get("/api/cobertura/prestadores/1/valores").session(s)).andExpect(status().isOk());
        // pero no modifica nada
        mvc.perform(post("/api/cobertura/practicas").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
        mvc.perform(put("/api/cobertura/practicas/1").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
        mvc.perform(post("/api/cobertura/planes").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
        mvc.perform(post("/api/cobertura/prestadores").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
    }

    @Test
    void medicoAuditorSoloConsulta() throws Exception {
        MockHttpSession s = ingresar("ludovico", "ludovico2026");
        mvc.perform(get("/api/cobertura/practicas").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/cobertura/coseguro" + COSEGURO).session(s)).andExpect(status().isOk());
        mvc.perform(put("/api/cobertura/practicas/1").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cobertura/prestadores/1/valores").session(s)).andExpect(status().isForbidden());
    }

    @Test
    void liquidacionesGestionaCoberturaYVeLosValores() throws Exception {
        MockHttpSession s = ingresar("gisela", "gisela2026");
        mvc.perform(get("/api/cobertura/prestadores/1/valores").param("fecha", "2026-08-15").session(s)).andExpect(status().isOk());
        mvc.perform(post("/api/cobertura/practicas").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/cobertura/practicas/1").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/cobertura/planes").session(s).with(csrf()).contentType(JSON).content(VACIO)).andExpect(status().isBadRequest());
    }

    @Test
    void rubroInvalidoEsRechazadoAlModificarUnaPractica() throws Exception {
        MockHttpSession s = ingresar("silvina", "silvina2026");
        String cuerpo = "{\"codigoCmm\":\"420101\",\"descripcion\":\"Consulta\",\"rubro\":\"CIRUGIA\",\"requiereAuditoria\":false}";
        // Un rubro inexistente es una regla de negocio: el sistema responde 422 (ManejadorErrores)
        mvc.perform(put("/api/cobertura/practicas/1").session(s).with(csrf()).contentType(JSON).content(cuerpo))
                .andExpect(status().isUnprocessableEntity());
    }
}
