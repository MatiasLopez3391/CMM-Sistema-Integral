package ar.org.cmm.comun;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Registro de accesos y cambios relevantes (requisito transversal de la Etapa 2). */
@Getter
@Setter
@Entity
@Table(name = "auditoria_cambio")
public class AuditoriaCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entidad;
    private Long entidadId;
    private String accion;
    private String detalle;
    private String usuario;
    private LocalDateTime fecha;
}
