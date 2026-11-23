package ar.org.cmm.facturacion;

import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.AfiliadoConsulta.AfiliadoResumen;
import ar.org.cmm.facturacion.FacturacionDtos.MorosoDto;
import ar.org.cmm.facturacion.FacturacionDtos.ResultadoAvisos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Avisos por mail de deuda, a uno o a todos los morosos del listado (T51). El asunto y el texto se editan
 * desde la pantalla de Morosos (parámetros aviso.deuda.*).
 * Si no hay servidor SMTP configurado (SMTP_HOST), el aviso queda registrado con estado SIN_SMTP
 * para que se vea en pantalla qué se hubiera enviado.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificacionRepository repo;
    private final ReportesFacturacionService reportes;
    private final AfiliadoConsulta afiliados;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final AuditoriaService auditoria;
    private final ParametroService parametros;
    private final String smtpHost;
    private final String remitente;

    public NotificacionService(NotificacionRepository repo, ReportesFacturacionService reportes, AfiliadoConsulta afiliados,
                               ObjectProvider<JavaMailSender> mailSender, AuditoriaService auditoria, ParametroService parametros,
                               @Value("${spring.mail.host:}") String smtpHost,
                               @Value("${app.mail-remitente}") String remitente) {
        this.repo = repo;
        this.reportes = reportes;
        this.afiliados = afiliados;
        this.mailSender = mailSender;
        this.auditoria = auditoria;
        this.parametros = parametros;
        this.smtpHost = smtpHost;
        this.remitente = remitente;
    }

    public boolean smtpConfigurado() {
        return smtpHost != null && !smtpHost.isBlank() && mailSender.getIfAvailable() != null;
    }

    @Transactional
    public ResultadoAvisos avisarDeuda(List<Long> afiliadoIds) {
        LocalDate hoy = LocalDate.now();
        Map<Long, MorosoDto> morosos = reportes.morosos(hoy).stream()
                .collect(Collectors.toMap(MorosoDto::afiliadoId, Function.identity()));
        NumberFormat moneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
        int enviados = 0, sinSmtp = 0, sinEmail = 0, errores = 0;
        for (Long id : afiliadoIds) {
            MorosoDto m = morosos.get(id);
            if (m == null) {
                continue; // ya no adeuda
            }
            AfiliadoResumen a = afiliados.obtener(id);
            Notificacion n = new Notificacion();
            n.setAfiliadoId(id);
            n.setDestinatario(a.email());
            n.setTipo("AVISO_DEUDA");
            n.setFechaCreacion(LocalDateTime.now());
            n.setAsunto(parametros.texto(ParametroService.AVISO_ASUNTO));
            n.setCuerpo(parametros.textoLargo(ParametroService.AVISO_TEXTO)
                    .replace("{nombre}", a.apellidoNombre())
                    .replace("{fecha}", hoy.format(FECHA))
                    .replace("{cuotas}", String.valueOf(m.cuotasVencidas()))
                    .replace("{deuda}", moneda.format(m.deudaVencida()))
                    .replace("{vencimiento}", m.vencimientoMasAntiguo().format(FECHA)));
            if (a.email() == null || a.email().isBlank()) {
                n.setEstado("SIN_EMAIL");
                sinEmail++;
            } else if (!smtpConfigurado()) {
                n.setEstado("SIN_SMTP");
                sinSmtp++;
            } else {
                try {
                    SimpleMailMessage msg = new SimpleMailMessage();
                    msg.setFrom(remitente);
                    msg.setTo(a.email());
                    msg.setSubject(n.getAsunto());
                    msg.setText(n.getCuerpo());
                    mailSender.getObject().send(msg);
                    n.setEstado("ENVIADA");
                    n.setFechaEnvio(LocalDateTime.now());
                    enviados++;
                } catch (Exception e) {
                    log.warn("No se pudo enviar el aviso a {}", a.email(), e);
                    n.setEstado("ERROR");
                    n.setError(e.getMessage() != null && e.getMessage().length() > 500 ? e.getMessage().substring(0, 500) : e.getMessage());
                    errores++;
                }
            }
            repo.save(n);
        }
        auditoria.registrar("notificacion", 0L, "AVISO_DEUDA",
                "Enviados " + enviados + ", sin SMTP " + sinSmtp + ", sin email " + sinEmail + ", errores " + errores);
        return new ResultadoAvisos(enviados, sinSmtp, sinEmail, errores);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> recientes() {
        return repo.findTop200ByOrderByFechaCreacionDesc();
    }
}
