package ar.org.cmm.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CargoCuotaRepository extends JpaRepository<CargoCuota, Long> {

    Optional<CargoCuota> findByOrigenAndOrigenRef(String origen, String origenRef);

    /** Cargos que entran en la cuota del período: los de ese mes y los pendientes de meses anteriores. */
    @Query("""
            select c from CargoCuota c
            where c.titularId = :titularId and c.estado = 'PENDIENTE' and c.periodoDestino <= :periodo
            order by c.fecha, c.id""")
    List<CargoCuota> pendientesParaCuota(@Param("titularId") Long titularId, @Param("periodo") String periodo);

    List<CargoCuota> findByTitularIdAndEstadoOrderByFechaAscIdAsc(Long titularId, String estado);

    List<CargoCuota> findByComprobanteIdOrderByFechaAscIdAsc(Long comprobanteId);

    @Query("""
            select c from CargoCuota c
            where c.periodoDestino between :desde and :hasta
              and (:estado is null or c.estado = :estado)
              and (:titularId is null or c.titularId = :titularId)
            order by c.periodoDestino desc, c.fecha desc, c.id desc""")
    List<CargoCuota> buscar(@Param("desde") String desde, @Param("hasta") String hasta, @Param("estado") String estado,
                            @Param("titularId") Long titularId);
}
