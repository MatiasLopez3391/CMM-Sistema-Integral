package ar.org.cmm.facturacion;

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

/** Qué parte de un pago canceló qué comprobante. */
@Getter
@Setter
@Entity
@Table(name = "imputacion")
public class Imputacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pago_id")
    private Long pagoId;

    @Column(name = "comprobante_id")
    private Long comprobanteId;

    private BigDecimal importe;
    private LocalDate fecha;
}
