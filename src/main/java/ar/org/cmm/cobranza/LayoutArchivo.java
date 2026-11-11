package ar.org.cmm.cobranza;

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

/**
 * Diseño de archivo de cobranza, configurable sin tocar código.
 *
 * <p>ANCHO_FIJO: las posiciones son de carácter (desde 1) y los largos son obligatorios.
 * <br>DELIMITADO: las posiciones son números de columna (desde 1) y los largos no se usan.
 */
@Getter
@Setter
@Entity
@Table(name = "layout_archivo")
public class LayoutArchivo {

    public static final String ANCHO_FIJO = "ANCHO_FIJO";
    public static final String DELIMITADO = "DELIMITADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "medio_cobro_id")
    private MedioCobro medioCobro;

    private String nombre;
    private String formato;
    private String delimitador;
    /** Si se indica, solo se procesan las líneas que empiezan con este prefijo (registros de detalle). */
    private String prefijoRegistro;
    private int lineasEncabezado;

    private int afiliadoPos;
    private Integer afiliadoLargo;
    private int fechaPos;
    private Integer fechaLargo;
    private String fechaFormato;
    private int importePos;
    private Integer importeLargo;
    /** Cantidad de decimales implícitos (ej. 2: "000000012345" = 123,45). */
    private int importeDecimalesImpl;
    private Integer referenciaPos;
    private Integer referenciaLargo;

    private boolean provisorio;
    private String observaciones;
    private boolean activo = true;
}
