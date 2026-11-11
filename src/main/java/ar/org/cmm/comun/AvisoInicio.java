package ar.org.cmm.comun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Informa en consola, en cada arranque, cuándo el sistema está listo para usarse. */
@Component
public class AvisoInicio {

    private static final Logger log = LoggerFactory.getLogger(AvisoInicio.class);

    private final Environment env;

    public AvisoInicio(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void listo() {
        log.info("==> Sistema listo. Abra http://localhost:{} en el navegador. (Ctrl + C para detenerlo)",
                env.getProperty("local.server.port", env.getProperty("server.port", "8080")));
    }
}
