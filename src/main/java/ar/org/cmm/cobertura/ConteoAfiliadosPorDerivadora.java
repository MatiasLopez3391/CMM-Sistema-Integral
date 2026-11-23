package ar.org.cmm.cobertura;

import java.util.Map;

/**
 * Puerto: cuántos afiliados activos tiene cada derivadora. El módulo Cobertura no debe consultar
 * las tablas de Afiliados directamente; cuando F1 publique este dato en AfiliadoConsulta
 * solo hay que cambiar el adaptador (ver ConteoAfiliadosPorDerivadoraJdbc).
 */
public interface ConteoAfiliadosPorDerivadora {

    /** Mapa derivadoraId -> cantidad de afiliados activos. */
    Map<Long, Long> activosPorDerivadora();
}
