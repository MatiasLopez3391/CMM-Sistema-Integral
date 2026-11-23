package ar.org.cmm.facturacion;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprobante interno NO fiscal (H2: por ahora no se emite factura electrónica).
 * Se emite al titular por todo el grupo familiar. Tipos: cuota mensual, nota de débito (suma deuda)
 * y nota de crédito (cancela deuda o queda a favor), estas dos últimas para corregir errores (T46).
 */
@Getter
@Setter
@Entity
@Table(name = "comprobante")
public class Comprobante extends Auditable {

    public static final String TIPO_CUOTA = "CUOTA";
    public static final String TIPO_NOTA_DEBITO = "NOTA_DEBITO";
    public static final String TIPO_NOTA_CREDITO = "NOTA_CREDITO";
    /** Estado de una nota de crédito: su importe ya es un crédito en la cuenta corriente. */
    public static final String EMITIDA = "EMITIDA";
    public static final String PENDIENTE = "PENDIENTE";
    public static final String PARCIAL = "PARCIAL";
    public static final String PAGADO = "PAGADO";
    public static final String ANULADO = "ANULADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;
    private String numero;

    @Column(name = "afiliado_id")
    private Long afiliadoId;

    /** AAAAMM */
    private String periodo;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private BigDecimal total;
    private BigDecimal saldo;
    private String estado;
    private String observaciones;

    /** Comprobante que corrige una nota de crédito o débito (opcional). */
    private Long comprobanteAsociadoId;

    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ComprobanteItem> items = new ArrayList<>();

    public void agregarItem(ComprobanteItem item) {
        item.setComprobante(this);
        items.add(item);
    }

    public boolean esNotaCredito() {
        return TIPO_NOTA_CREDITO.equals(tipo);
    }

    public void aplicarPago(BigDecimal importe) {
        saldo = saldo.subtract(importe);
        estado = saldo.signum() <= 0 ? PAGADO : PARCIAL;
    }
}
