package ar.org.cmm.cobertura;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "prestador")
public class Prestador extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String cuit;
    /** CLINICA, INSTITUCION, PROFESIONAL, LABORATORIO, FARMACIA... */
    private String tipo;
    private String email;
    private boolean activo = true;
}
