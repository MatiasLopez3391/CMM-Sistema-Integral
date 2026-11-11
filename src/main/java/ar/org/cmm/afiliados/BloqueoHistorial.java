package ar.org.cmm.afiliados;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "bloqueo_historial")
public class BloqueoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "afiliado_id")
    private Afiliado afiliado;

    /** BLOQUEO o DESBLOQUEO */
    private String accion;
    /** Ej.: "Bloqueado por falta de pago" (misma marca que el sistema actual). */
    private String marca;
    private String motivo;
    private String usuario;
    private LocalDateTime fecha;
}
