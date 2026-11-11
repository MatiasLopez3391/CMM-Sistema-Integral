package ar.org.cmm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sistema Integral de Gestión del Círculo Médico de Mendoza.
 *
 * <p>Organización por frentes (ver "Plan de trabajo"): cada paquete es dueño de sus tablas,
 * su API y sus pantallas. Entre frentes solo se usan las interfaces de {@code ar.org.cmm.contratos}.
 */
@SpringBootApplication
public class SistemaIntegralApplication {

    public static void main(String[] args) {
        SpringApplication.run(SistemaIntegralApplication.class, args);
    }
}
