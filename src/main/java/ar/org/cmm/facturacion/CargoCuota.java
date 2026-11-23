package ar.org.cmm.facturacion;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cargo que se suma a la próxima cuota del grupo familiar (por ahora, coseguros de órdenes).
 * Queda PENDIENTE hasta que la generación de cuotas lo incluye en un comprobante (FACTURADO).
 */
@Getter
@Setter
@Entity
@Table(name = "cargo_cuota")
public class CargoCuota extends Auditable {

    public static final String TIPO_COSEGURO = "COSEGURO";
    public static final String ORIGEN_ORDEN = "ORDEN";
    public static final String ORIGEN_MANUAL = "MANUAL";
    public static final String PENDIENTE = "PENDIENTE";
    public static final String FACTURADO = "FACTURADO";
    public static final String ANULADO = "ANULADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Titular del grupo al registrar el cargo: la cuota se emite a su nombre. */
    private Long titularId;
    /** Integrante que usó la prestación. */
    private Long afiliadoId;
    private String tipo;
    private String concepto;
    private LocalDate fecha;
    private BigDecimal importe;
    /** AAAAMM de la cuota en la que se cobra. */
    private String periodoDestino;
    private String origen;
    private String origenRef;
    private String estado;
    private Long comprobanteId;
    private String motivoAnulacion;
}
