package ar.org.cmm.contratos;

/**
 * Contrato publicado por F3 para que otros frentes lean parámetros de negocio editables desde Configuración sin
 * depender del módulo de Facturación (lo usa F5 para el tope mensual y el vencimiento de las órdenes).
 */
public interface ParametroConsulta {

    /** Valor entero del parámetro; si no existe o no es un número, devuelve {@code porDefecto}. */
    int entero(String clave, int porDefecto);
}
