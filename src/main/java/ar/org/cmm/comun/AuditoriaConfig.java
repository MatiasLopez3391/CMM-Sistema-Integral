package ar.org.cmm.comun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditoriaConfig {

    @Bean
    public AuditorAware<String> auditorAware(UsuarioActualProvider usuarioActual) {
        return () -> Optional.of(usuarioActual.usuario());
    }
}
