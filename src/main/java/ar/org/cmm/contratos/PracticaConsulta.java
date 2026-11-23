package ar.org.cmm.contratos;

import java.util.List;
import java.util.Optional;

/** Contrato publicado por F2 para leer prácticas sin depender del módulo Cobertura (lo usa F5 al emitir órdenes). */
public interface PracticaConsulta {

    PracticaResumen obtener(Long practicaId);

    Optional<PracticaResumen> porCodigoCmm(String codigoCmm);

    /** Busca por código CMM o por descripción (sin distinguir mayúsculas). */
    List<PracticaResumen> buscar(String texto);

    record PracticaResumen(Long id, String codigoCmm, String codigoNomenclador, String descripcion, String rubro,
                           boolean requiereAuditoria) {
    }
}
