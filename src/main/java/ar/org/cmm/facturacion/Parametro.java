package ar.org.cmm.facturacion;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "parametro")
public class Parametro {

    @Id
    private String clave;

    private String valor;
    private String descripcion;
}
