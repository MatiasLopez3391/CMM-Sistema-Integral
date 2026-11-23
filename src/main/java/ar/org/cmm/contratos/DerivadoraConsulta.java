package ar.org.cmm.contratos;

import java.time.LocalDate;
import java.util.List;

/** Contrato publicado por F2 para leer derivadoras (F1 lo usa en lugar de DerivadoraRepository). */
public interface DerivadoraConsulta {

    List<DerivadoraResumen> todas();

    DerivadoraResumen obtener(Long derivadoraId);

    record DerivadoraResumen(Long id, String nombre, String cuit, String convenio, LocalDate vigenteDesde,
                             LocalDate vigenteHasta, boolean activa) {
    }
}
