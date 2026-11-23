package ar.org.cmm.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Errores de seguridad con el mismo formato que el resto de la API (el frontend muestra el campo "detail"). */
@Component
public class RespuestasSeguridad {

    private final ObjectMapper mapper;

    public RespuestasSeguridad(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void escribir(HttpServletResponse response, int status, String titulo, String detalle) throws IOException {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("title", titulo);
        cuerpo.put("status", status);
        cuerpo.put("detail", detalle);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getWriter(), cuerpo);
    }
}
