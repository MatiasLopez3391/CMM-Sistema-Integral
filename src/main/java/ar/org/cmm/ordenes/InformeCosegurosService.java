
package ar.org.cmm.ordenes;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class InformeCosegurosService {

    private final OrdenRepository ordenes;

    public InformeCosegurosService(OrdenRepository ordenes) {
        this.ordenes = ordenes;
    }

    public record DetalleOrden(
            Long ordenId,
            String numero,
            Long afiliadoId,
            LocalDate fecha,
            String estado,
            BigDecimal valorConvenido,
            BigDecimal cargoEntidad,
            BigDecimal coseguro
    ) {
    }

    public record InformeResponse(
            LocalDate desde,
            LocalDate hasta,
            int cantidadOrdenes,
            BigDecimal totalValorConvenido,
            BigDecimal totalCargoEntidad,
            BigDecimal totalCoseguro,
            List<DetalleOrden> detalle
    ) {
    }

    @Transactional(readOnly = true)
    public InformeResponse generar(
            LocalDate desde,
            LocalDate hasta,
            Long afiliadoId) {

        if (desde == null || hasta == null) {
            throw new ar.org.cmm.comun.ReglaNegocioException(
                    "Debe indicar las fechas desde y hasta"
            );
        }

        if (desde.isAfter(hasta)) {
            throw new ar.org.cmm.comun.ReglaNegocioException(
                    "La fecha desde no puede ser posterior a hasta"
            );
        }

        List<DetalleOrden> detalle = ordenes.findAll()
                .stream()
                .filter(o ->
                        !o.getFechaEmision().isBefore(desde)
                        && !o.getFechaEmision().isAfter(hasta)
                )
                .filter(o ->
                        afiliadoId == null
                        || afiliadoId.equals(o.getAfiliadoId())
                )
                .filter(o ->
                        !OrdenEstado.RECHAZADA.name()
                                .equals(o.getEstado())
                        && !OrdenEstado.ANULADA.name()
                                .equals(o.getEstado())
                )
                .map(o -> new DetalleOrden(
                        o.getId(),
                        o.getNumero(),
                        o.getAfiliadoId(),
                        o.getFechaEmision(),
                        o.getEstado(),
                        o.getTotalValorConvenido(),
                        o.getTotalCargoEntidad(),
                        o.getTotalCoseguro()
                ))
                .sorted((a, b) ->
                        b.fecha().compareTo(a.fecha())
                )
                .toList();

        BigDecimal totalValor = BigDecimal.ZERO;
        BigDecimal totalEntidad = BigDecimal.ZERO;
        BigDecimal totalCoseguro = BigDecimal.ZERO;

        for (DetalleOrden d : detalle) {
            totalValor = totalValor.add(d.valorConvenido());
            totalEntidad = totalEntidad.add(d.cargoEntidad());
            totalCoseguro = totalCoseguro.add(d.coseguro());
        }

        return new InformeResponse(
                desde,
                hasta,
                detalle.size(),
                totalValor,
                totalEntidad,
                totalCoseguro,
                detalle
        );
    }
}
