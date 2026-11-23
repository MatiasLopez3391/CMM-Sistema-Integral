package ar.org.cmm.cobertura.coseguro;

import ar.org.cmm.cobertura.Plan;
import ar.org.cmm.cobertura.Practica;
import ar.org.cmm.cobertura.ValorConvenido;
import ar.org.cmm.contratos.CoseguroServicio.CalculoCoseguro;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regla acordada (H12): coseguro = valor convenido × (1 − % de cobertura del plan para el rubro).
 * Es la estrategia por defecto: aplica a todo y va última en el orden.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CoseguroPorCobertura implements CalculadoraCoseguro {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final CoberturaDePlan cobertura;

    public CoseguroPorCobertura(CoberturaDePlan cobertura) {
        this.cobertura = cobertura;
    }

    @Override
    public boolean aplicaA(Plan plan, Practica practica) {
        return true;
    }

    @Override
    public CalculoCoseguro calcular(Plan plan, Practica practica, ValorConvenido valor) {
        BigDecimal porcentaje = cobertura.porcentaje(plan, practica.getRubro());
        BigDecimal importe = valor.getImporte();
        BigDecimal aCargoEntidad = importe.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
        BigDecimal coseguro = importe.subtract(aCargoEntidad);
        return new CalculoCoseguro(valor.getPrestador().getId(), practica.getId(), plan.getId(), plan.getCodigo(), plan.getNombre(),
                practica.getRubro(), valor.getVigenteDesde(), importe, porcentaje, aCargoEntidad, coseguro);
    }
}
