package ar.org.cmm.cobertura;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Plan = código + nombre + nivel de cobertura (H1). Entidad núcleo (v0-base). */
@Getter
@Setter
@Entity
@Table(name = "plan")
public class Plan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private String nombre;
    private BigDecimal coberturaPorcentaje;
    private boolean activo = true;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rubro")
    private List<CoberturaRubro> coberturasPorRubro = new ArrayList<>();
}
