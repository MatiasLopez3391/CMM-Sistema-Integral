package ar.org.cmm.facturacion;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Variación mensual del IPC (INDEC) en %, usada para actualizar los valores de cuota (T44). */
@Getter
@Setter
@Entity
@Table(name = "indice_ipc")
public class IndiceIpc extends Auditable {

    /** AAAAMM del mes medido. */
    @Id
    private String periodo;
    /** Variación respecto del mes anterior, en % (ej. 2.300 = 2,3 %). */
    private BigDecimal variacion;
    /** API o MANUAL. */
    private String origen;
    /** Vigencia de los valores de cuota creados al aplicarlo (null = todavía no se aplicó). */
    private LocalDate aplicadoVigencia;
}
