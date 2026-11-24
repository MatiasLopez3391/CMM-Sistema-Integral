
package ar.org.cmm.ordenes;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orden")
@Getter
@Setter
@NoArgsConstructor
public class Orden extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String numero;

    @Column(name = "afiliado_id", nullable = false)
    private Long afiliadoId;

    @Column(name = "prestador_id", nullable = false)
    private Long prestadorId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(name = "total_valor_convenido",
            nullable = false, precision = 15, scale = 2)
    private BigDecimal totalValorConvenido = BigDecimal.ZERO;

    @Column(name = "total_coseguro",
            nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCoseguro = BigDecimal.ZERO;

    @Column(name = "total_cargo_entidad",
            nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCargoEntidad = BigDecimal.ZERO;

    @Column(name = "emitida_por", nullable = false, length = 100)
    private String emitidaPor;

    /** Por qué fue a auditoría: práctica que lo requiere, tope mensual o pedido del médico. */
    @Column(name = "motivo_auditoria", length = 300)
    private String motivoAuditoria;

    @Column(name = "medico_solicitante", length = 150)
    private String medicoSolicitante;

    @Column(length = 1000)
    private String observaciones;

    /** Vacía mientras la orden está en auditoría. */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    /** Última decisión del auditor (o anulación): con este dato se avisa a Mesa de Entradas. */
    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @OrderBy("id")
    @OneToMany(
            mappedBy = "orden",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrdenItem> items = new ArrayList<>();

    public void agregarItem(OrdenItem item) {
        item.setOrden(this);
        items.add(item);
    }
}
