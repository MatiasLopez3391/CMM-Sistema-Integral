
package ar.org.cmm.ordenes;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_medica")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(name = "usuario_auditor", nullable = false, length = 100)
    private String usuarioAuditor;

    @Column(nullable = false, length = 30)
    private String decision;

    @Column(length = 2000)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
