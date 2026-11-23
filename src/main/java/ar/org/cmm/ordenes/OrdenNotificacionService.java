
package ar.org.cmm.ordenes;

import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.facturacion.Notificacion;
import ar.org.cmm.facturacion.NotificacionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrdenNotificacionService {

    private static final Logger log =
            LoggerFactory.getLogger(OrdenNotificacionService.class);

    private final NotificacionRepository notificaciones;
    private final AfiliadoConsulta afiliados;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String smtpHost;
    private final String remitente;

    public OrdenNotificacionService(
            NotificacionRepository notificaciones,
            AfiliadoConsulta afiliados,
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.mail-remitente}") String remitente) {

        this.notificaciones = notificaciones;
        this.afiliados = afiliados;
        this.mailSender = mailSender;
        this.smtpHost = smtpHost;
        this.remitente = remitente;
    }

    public void notificarDecision(Orden orden) {

        var afiliado = afiliados.obtener(orden.getAfiliadoId());

        Notificacion n = new Notificacion();

        n.setAfiliadoId(orden.getAfiliadoId());
        n.setDestinatario(afiliado.email());
        n.setTipo("AUDITORIA_ORDEN");
        n.setFechaCreacion(LocalDateTime.now());

        n.setAsunto(
                "Resultado de auditoria medica - " + orden.getNumero()
        );

        n.setCuerpo(
                "Estimado/a " + afiliado.apellidoNombre() + ":\n\n"
                + "Le informamos que su orden medica "
                + orden.getNumero()
                + " se encuentra en estado "
                + orden.getEstado()
                + ".\n\n"
                + "CMM"
        );

        if (afiliado.email() == null ||
                afiliado.email().isBlank()) {

            n.setEstado("SIN_EMAIL");

        } else if (smtpHost == null ||
                smtpHost.isBlank() ||
                mailSender.getIfAvailable() == null) {

            n.setEstado("SIN_SMTP");

        } else {

            try {

                SimpleMailMessage mensaje = new SimpleMailMessage();

                mensaje.setFrom(remitente);
                mensaje.setTo(afiliado.email());
                mensaje.setSubject(n.getAsunto());
                mensaje.setText(n.getCuerpo());

                mailSender.getObject().send(mensaje);

                n.setEstado("ENVIADA");
                n.setFechaEnvio(LocalDateTime.now());

            } catch (Exception e) {

                log.warn(
                        "No se pudo notificar la orden {}",
                        orden.getNumero(),
                        e
                );

                n.setEstado("ERROR");

                String error = e.getMessage();

                n.setError(
                        error != null && error.length() > 500
                                ? error.substring(0, 500)
                                : error
                );
            }
        }

        notificaciones.save(n);
    }
}
