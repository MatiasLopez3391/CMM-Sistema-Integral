
package ar.org.cmm.ordenes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "orden_item")
@Getter
@Setter
@NoArgsConstructor
public class OrdenItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(name = "practica_id", nullable = false)
    private Long practicaId;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "valor_convenido",
            nullable = false, precision = 15, scale = 2)
    private BigDecimal valorConvenido;

    @Column(name = "porcentaje_cobertura",
            nullable = false, precision = 10, scale = 4)
    private BigDecimal porcentajeCobertura;

    @Column(name = "cargo_entidad",
            nullable = false, precision = 15, scale = 2)
    private BigDecimal cargoEntidad;

    @Column(name = "coseguro_afiliado",
            nullable = false, precision = 15, scale = 2)
    private BigDecimal coseguroAfiliado;

    @Column(name = "requiere_auditoria", nullable = false)
    private boolean requiereAuditoria;
}
