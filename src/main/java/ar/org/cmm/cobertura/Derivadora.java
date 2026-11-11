package ar.org.cmm.cobertura;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Obra social de origen de afiliados monotributistas (H11). Sin facturación de aportes por ahora. */
@Getter
@Setter
@Entity
@Table(name = "derivadora")
public class Derivadora extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String cuit;
    private String convenio;
    private LocalDate vigenteDesde;
    private LocalDate vigenteHasta;
    private boolean activa = true;
}
