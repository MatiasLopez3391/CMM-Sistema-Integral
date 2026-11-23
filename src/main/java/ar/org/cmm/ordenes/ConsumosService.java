
package ar.org.cmm.ordenes;

import ar.org.cmm.contratos.ConsumosConsulta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsumosService implements ConsumosConsulta {

    private final OrdenRepository ordenes;

    public ConsumosService(OrdenRepository ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsumoResumen> obtenerPorAfiliado(Long afiliadoId) {

        return ordenes
                .findByAfiliadoIdOrderByFechaEmisionDesc(afiliadoId)
                .stream()
                .filter(orden ->
                        "EMITIDA".equals(orden.getEstado())
                        || "AUTORIZADA".equals(orden.getEstado())
                )
                .map(orden -> new ConsumoResumen(
                        orden.getId(),
                        orden.getNumero(),
                        orden.getAfiliadoId(),
                        orden.getFechaEmision(),
                        orden.getEstado(),
                        orden.getTotalValorConvenido(),
                        orden.getTotalCargoEntidad(),
                        orden.getTotalCoseguro()
                ))
                .toList();
    }
}
