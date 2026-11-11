package ar.org.cmm.cobranza;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.Column;
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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago recibido. Proviene de un archivo de ente de cobro (lote) o de ventanilla (con comprobante adjunto).
 * Estados: PENDIENTE (en previsualización), IMPUTADO, RECHAZADO.
 */
@Getter
@Setter
@Entity
@Table(name = "pago")
public class Pago extends Auditable {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String IMPUTADO = "IMPUTADO";
    public static final String RECHAZADO = "RECHAZADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteCobro lote;

    private Integer linea;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "medio_cobro_id")
    private MedioCobro medioCobro;

    @Column(name = "afiliado_id")
    private Long afiliadoId;

    private String nroAfiliadoInformado;
    private LocalDate fechaPago;
    private BigDecimal importe;
    private String referencia;
    private String estado;
    private String motivoRechazo;
    private String adjuntoNombre;
    private String adjuntoRuta;
    private String adjuntoTipo;
    private String observaciones;
}
