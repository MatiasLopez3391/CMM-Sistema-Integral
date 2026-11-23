package ar.org.cmm.cobertura.coseguro;

import ar.org.cmm.cobertura.Plan;
import ar.org.cmm.cobertura.Practica;
import ar.org.cmm.cobertura.ValorConvenido;
import ar.org.cmm.contratos.CoseguroServicio.CalculoCoseguro;

/**
 * Patrón Strategy: cada forma de calcular el coseguro es una implementación.
 * Para sumar una regla nueva (por ejemplo un monto fijo para ciertas prácticas) se crea otra
 * implementación con @Component y @Order menor; no se toca el resto del módulo.
 */
public interface CalculadoraCoseguro {

    /** Indica si esta estrategia corresponde a la combinación plan + práctica. */
    boolean aplicaA(Plan plan, Practica practica);

    CalculoCoseguro calcular(Plan plan, Practica practica, ValorConvenido valor);
}
