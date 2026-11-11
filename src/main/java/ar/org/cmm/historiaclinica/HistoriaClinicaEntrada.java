package ar.org.cmm.historiaclinica;

import ar.org.cmm.comun.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entrada de historia clínica. Se referencia al afiliado por id (no por entidad) para que el
 * módulo dependa solo del contrato {@code AfiliadoConsulta}.
 */
@Getter
@Setter
@Entity
@Table(name = "historia_clinica_entrada")
public class HistoriaClinicaEntrada extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id")
    private Long afiliadoId;

    private LocalDate fecha;
    /** CONSULTA, GUARDIA, INTERNACION, ESTUDIO, OTRO */
    private String tipo;
    private String profesional;
    private String diagnostico;
    private String detalle;
}
