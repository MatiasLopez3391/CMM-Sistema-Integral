package ar.org.cmm.facturacion;

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

/**
 * Concepto facturable asociado a una cuenta contable (base de los asientos para FEBBO).
 * tipo: CUOTA, ADICIONAL (se suma por integrante si incluirEnCuota), RECARGO, OTRO.
 */
@Getter
@Setter
@Entity
@Table(name = "codigo_servicio")
public class CodigoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private String descripcion;
    private String tipo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cuenta_contable_id")
    private CuentaContable cuentaContable;

    private BigDecimal importeFijo;
    private boolean incluirEnCuota;
    private boolean activo = true;
}
