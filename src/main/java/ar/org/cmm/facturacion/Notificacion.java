package ar.org.cmm.facturacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Aviso por mail. Estados: ENVIADA, ERROR, SIN_SMTP (registrada pero no enviada porque no hay
 * servidor de correo configurado), SIN_EMAIL (el afiliado no tiene correo).
 */
@Getter
@Setter
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id")
    private Long afiliadoId;

    private String destinatario;
    private String asunto;
    private String cuerpo;
    private String tipo;
    private String estado;
    private String error;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEnvio;
}
