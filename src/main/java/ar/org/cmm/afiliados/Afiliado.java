package ar.org.cmm.afiliados;

import ar.org.cmm.cobertura.Derivadora;
import ar.org.cmm.cobertura.Plan;
import ar.org.cmm.comun.Auditable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

/**
 * Afiliado (titular o integrante del grupo familiar). Entidad núcleo (v0-base).
 *
 * <p>Número de afiliado de 12 dígitos: 10 de base del grupo + 2 del integrante (00 = titular),
 * como en el sistema actual (ej. 1310899587 00).
 */
@Getter
@Setter
@Entity
@Table(name = "afiliado")
public class Afiliado extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nroAfiliado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_id")
    private Afiliado titular;

    @Enumerated(EnumType.STRING)
    private Parentesco parentesco;

    private String apellido;
    private String nombre;
    private String tipoDocumento;
    private String documento;
    private String cuil;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String email;
    private String telefono;
    private String domicilio;
    private String localidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id")
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "derivadora_id")
    private Derivadora derivadora;

    /** Código del medio de cobro habitual (PMC, RAPIPAGO, CAJA...). */
    private String viaCobro;

    @Enumerated(EnumType.STRING)
    private Carencia carencia;

    private String estadoEspecial;
    private LocalDate estadoEspecialDesde;
    private LocalDate estadoEspecialHasta;

    @Enumerated(EnumType.STRING)
    private EstadoAfiliado estado;

    private LocalDate fechaAlta;
    private LocalDate fechaBaja;
    private String motivoBaja;

    /** Marca de bloqueo (se guarda en el titular y aplica a todo el grupo). */
    private boolean bloqueado;

    public boolean esTitular() {
        return titular == null;
    }

    public Afiliado titularDelGrupo() {
        return titular != null ? titular : this;
    }

    public String apellidoNombre() {
        return apellido + ", " + nombre;
    }

    public int edad() {
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public boolean estadoEspecialVigente(LocalDate fecha) {
        return estadoEspecial != null
                && (estadoEspecialDesde == null || !fecha.isBefore(estadoEspecialDesde))
                && (estadoEspecialHasta == null || !fecha.isAfter(estadoEspecialHasta));
    }

    public enum Parentesco { TITULAR, CONYUGE, HIJO, OTRO }

    public enum EstadoAfiliado { ACTIVO, BAJA }

    public enum Carencia { COMUN, REDUCIDA, SIN_CARENCIA }
}
