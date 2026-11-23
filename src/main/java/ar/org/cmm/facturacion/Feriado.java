package ar.org.cmm.facturacion;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Día no hábil: si el vencimiento cae en uno, pasa al día hábil siguiente (T45). */
@Getter
@Setter
@Entity
@Table(name = "feriado")
public class Feriado extends Auditable {

    @Id
    private LocalDate fecha;
    private String descripcion;
    /** INICIAL (migración), MANUAL o API. */
    private String origen;
}
