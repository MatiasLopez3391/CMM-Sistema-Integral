package ar.org.cmm.facturacion;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Qué parte de una nota de crédito canceló qué comprobante (T46). */
@Getter
@Setter
@Entity
@Table(name = "aplicacion_credito")
public class AplicacionCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long notaCreditoId;
    private Long comprobanteId;
    private BigDecimal importe;
    private LocalDate fecha;
}
