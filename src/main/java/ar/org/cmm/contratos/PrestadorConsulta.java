package ar.org.cmm.contratos;

import java.util.List;

/** Contrato publicado por F2 para leer prestadores sin depender del módulo Cobertura. */
public interface PrestadorConsulta {

    List<PrestadorResumen> todos();

    PrestadorResumen obtener(Long prestadorId);

    record PrestadorResumen(Long id, String nombre, String cuit, String tipo, boolean activo) {
    }
}
