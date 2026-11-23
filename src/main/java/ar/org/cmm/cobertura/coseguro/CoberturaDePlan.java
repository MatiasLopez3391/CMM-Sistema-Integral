package ar.org.cmm.cobertura.coseguro;

import ar.org.cmm.cobertura.CoberturaRubro;
import ar.org.cmm.cobertura.Plan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Resuelve el % de cobertura de un plan para un rubro: la excepción por rubro si existe, o la general. */
@Component
public class CoberturaDePlan {

    public BigDecimal porcentaje(Plan plan, String rubro) {
        return plan.getCoberturasPorRubro().stream()
                .filter(c -> c.getRubro().equalsIgnoreCase(rubro))
                .map(CoberturaRubro::getPorcentaje)
                .findFirst()
                .orElse(plan.getCoberturaPorcentaje());
    }
}
