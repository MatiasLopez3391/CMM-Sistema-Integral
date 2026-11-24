
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
        notificarDecision(orden, null, java.util.List.of());
    }

    /** Avisa al afiliado el resultado de la auditoría, con el motivo y lo que cambió (M9). */
    public void notificarDecision(Orden orden, String motivo, java.util.List<String> cambios) {

        var afiliado = afiliados.obtener(orden.getAfiliadoId());

        Notificacion n = new Notificacion();
        n.setAfiliadoId(orden.getAfiliadoId());
        n.setDestinatario(afiliado.email());
        n.setTipo("AUDITORIA_ORDEN");
        n.setFechaCreacion(LocalDateTime.now());

        boolean rechazada = "RECHAZADA".equals(orden.getEstado());
        boolean parcial = !rechazada && cambios != null && !cambios.isEmpty();
        n.setAsunto("Resultado de la auditoría médica - Orden " + orden.getNumero());
        StringBuilder cuerpo = new StringBuilder()
                .append("Estimado/a ").append(afiliado.apellidoNombre()).append(":\n\n")
                .append("El médico auditor revisó su orden ").append(orden.getNumero()).append(".\n")
                .append("Resultado: ").append(orden.getEstado())
                .append(parcial ? " (con cambios en lo solicitado)" : "").append(".\n");
        if (parcial) {
            cuerpo.append("\nCambios:\n");
            cambios.forEach(c -> cuerpo.append("- ").append(c).append("\n"));
        }
        if (motivo != null && !motivo.isBlank()) {
            cuerpo.append("\nMotivo: ").append(motivo.trim()).append("\n");
        }
        if (!rechazada && orden.getFechaVencimiento() != null) {
            cuerpo.append("\nLa orden vence el ")
                    .append(orden.getFechaVencimiento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .append(". Si no la usa, recuerde anularla en Mesa de Entradas: de lo contrario el coseguro se cobra igual en la cuota.\n");
        }
        cuerpo.append("\nAnte cualquier consulta, comuníquese con Mesa de Entradas.\n\nCírculo Médico de Mendoza");
        n.setCuerpo(cuerpo.toString());

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
