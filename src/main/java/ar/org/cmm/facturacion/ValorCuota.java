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

/** Valor mensual de la cuota por integrante para un plan, desde una fecha. */
@Getter
@Setter
@Entity
@Table(name = "valor_cuota")
public class ValorCuota extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id")
    private Long planId;

    private LocalDate vigenteDesde;
    private BigDecimal importe;
    private String motivo;
}
