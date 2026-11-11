package ar.org.cmm.cobranza;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Archivo de cobranza importado. PREVISUALIZADO → CONFIRMADO (o DESCARTADO). */
@Getter
@Setter
@Entity
@Table(name = "lote_cobro")
public class LoteCobro extends Auditable {

    public static final String PREVISUALIZADO = "PREVISUALIZADO";
    public static final String CONFIRMADO = "CONFIRMADO";
    public static final String DESCARTADO = "DESCARTADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "medio_cobro_id")
    private MedioCobro medioCobro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_id")
    private LayoutArchivo layout;

    private String nombreArchivo;
    private String hashArchivo;
    private String estado;
    private int cantidadRegistros;
    private int cantidadOk;
    private int cantidadRechazados;
    private BigDecimal importeOk;
    private LocalDateTime fechaConfirmacion;
}
