package ar.org.cmm.cobertura;

import ar.org.cmm.comun.Auditable;
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

/** Valor que fija el prestador para una práctica desde una fecha (reemplaza las planillas Excel mensuales). */
@Getter
@Setter
@Entity
@Table(name = "valor_convenido")
public class ValorConvenido extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practica_id")
    private Practica practica;

    private LocalDate vigenteDesde;
    private BigDecimal importe;
    private String origen;
}
