package ar.org.cmm.cobertura;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Práctica del nomenclador (código CMM + código nomenclador). */
@Getter
@Setter
@Entity
@Table(name = "practica")
public class Practica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigoCmm;
    private String codigoNomenclador;
    private String descripcion;
    /** CONSULTA, ENFERMERIA, LABORATORIO, IMAGENES, MEDICAMENTOS, INTERNACION, ODONTOLOGIA, OTROS */
    private String rubro;
    private boolean requiereAuditoria;
}
