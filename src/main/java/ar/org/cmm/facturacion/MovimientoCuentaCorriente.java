package ar.org.cmm.facturacion;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Movimiento de cuenta corriente del titular: DEBE (cuotas, recargos) o HABER (pagos). */
@Getter
@Setter
@Entity
@Table(name = "movimiento_cc")
public class MovimientoCuentaCorriente extends Auditable {

    public static final String DEBE = "DEBE";
    public static final String HABER = "HABER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id")
    private Long afiliadoId;

    private LocalDate fecha;
    private String tipo;
    private String concepto;

    @Column(name = "comprobante_id")
    private Long comprobanteId;

    @Column(name = "pago_id")
    private Long pagoId;

    private String cuentaContrapartida;

    private BigDecimal importe;
}
