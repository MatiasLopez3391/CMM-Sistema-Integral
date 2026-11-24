
package ar.org.cmm.contratos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ConsumosConsulta {

    List<ConsumoResumen> obtenerPorAfiliado(Long afiliadoId);

    record ConsumoResumen(
            Long ordenId,
            String numeroOrden,
            Long afiliadoId,
            LocalDate fecha,
            String estado,
            BigDecimal valorConvenido,
            BigDecimal cargoEntidad,
            BigDecimal coseguro
    ) {
    }
}
