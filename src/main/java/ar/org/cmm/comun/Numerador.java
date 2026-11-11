package ar.org.cmm.comun;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "numerador")
public class Numerador {

    @Id
    private String clave;

    private long ultimo;
}
