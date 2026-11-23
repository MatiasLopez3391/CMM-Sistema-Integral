package ar.org.cmm.seguridad;

import ar.org.cmm.comun.Rol;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/** Usuario ya autenticado: lo que Spring Security guarda en la sesión. */
public class UsuarioAutenticado implements UserDetails {

    private final String username;
    private final String nombre;
    private final String claveHash;
    private final boolean activo;
    private final Set<Rol> roles;

    public UsuarioAutenticado(Usuario u) {
        this.username = u.getUsername();
        this.nombre = u.getNombre();
        this.claveHash = u.getClaveHash();
        this.activo = u.isActivo();
        this.roles = u.getRoles().isEmpty() ? EnumSet.noneOf(Rol.class) : EnumSet.copyOf(u.getRoles());
    }

    public String getNombre() {
        return nombre;
    }

    public Set<Rol> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList();
    }

    @Override
    public String getPassword() {
        return claveHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
