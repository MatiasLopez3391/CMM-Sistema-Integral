package ar.org.cmm.seguridad;

import ar.org.cmm.comun.Rol;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * El ingreso (POST /api/auth/login) y la salida (POST /api/auth/logout) los resuelve Spring Security
 * (ver {@link SeguridadConfig}). Acá solo se informa quién tiene la sesión abierta.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record SesionDto(String usuario, String nombre, List<Rol> roles) {}

    @GetMapping("/yo")
    public SesionDto yo(Authentication auth) {
        UsuarioAutenticado u = (UsuarioAutenticado) auth.getPrincipal();
        return new SesionDto(u.getUsername(), u.getNombre(), u.getRoles().stream().sorted().toList());
    }
}
