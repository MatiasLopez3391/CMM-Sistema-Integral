package ar.org.cmm.ordenes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByNumero(String numero);

    List<Orden> findByAfiliadoIdOrderByFechaEmisionDesc(Long afiliadoId);

    List<Orden> findByEstadoOrderByFechaEmisionDesc(String estado);

    boolean existsByNumero(String numero);

    /** Órdenes del afiliado en el mes que cuentan para el tope (M11). */
    long countByAfiliadoIdAndFechaEmisionBetweenAndEstadoIn(Long afiliadoId, LocalDate desde, LocalDate hasta,
                                                            Collection<String> estados);

    @Query("""
            select o from Orden o
            where (:afiliadoId is null or o.afiliadoId = :afiliadoId)
              and (:estado is null or o.estado = :estado)
              and (:numero is null or o.numero like concat('%', :numero, '%'))
              and o.fechaEmision between :desde and :hasta
            order by o.fechaEmision desc, o.id desc
            """)
    List<Orden> buscar(@Param("afiliadoId") Long afiliadoId, @Param("estado") String estado, @Param("numero") String numero,
                       @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Resueltas por auditoría después de un momento dado (aviso a Mesa de Entradas). */
    List<Orden> findByFechaResolucionAfterAndEstadoInOrderByFechaResolucionAsc(LocalDateTime desde, Collection<String> estados);

    /** Últimas decisiones del auditor. */
    List<Orden> findTop50ByFechaResolucionIsNotNullAndEstadoInOrderByFechaResolucionDesc(Collection<String> estados);
}
