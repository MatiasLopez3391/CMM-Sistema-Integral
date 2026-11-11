package ar.org.cmm.facturacion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    boolean existsByAfiliadoIdAndPeriodoAndTipo(Long afiliadoId, String periodo, String tipo);

    /** Comprobantes con saldo, del más antiguo al más nuevo (orden de imputación). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from Comprobante c
            where c.afiliadoId = :afiliadoId and c.saldo > 0 and c.estado <> 'ANULADO'
            order by c.fechaVencimiento, c.id""")
    List<Comprobante> pendientesParaImputar(@Param("afiliadoId") Long afiliadoId);

    @Query("""
            select c from Comprobante c
            where c.afiliadoId = :afiliadoId and c.saldo > 0 and c.estado <> 'ANULADO' and c.fechaVencimiento < :fecha
            order by c.fechaVencimiento""")
    List<Comprobante> vencidosImpagos(@Param("afiliadoId") Long afiliadoId, @Param("fecha") LocalDate fecha);

    @Query("""
            select c from Comprobante c
            where c.saldo > 0 and c.estado <> 'ANULADO' and c.fechaVencimiento < :fecha
            order by c.afiliadoId, c.fechaVencimiento""")
    List<Comprobante> todosVencidosImpagos(@Param("fecha") LocalDate fecha);

    @EntityGraph(attributePaths = {"items", "items.codigoServicio"})
    Optional<Comprobante> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items", "items.codigoServicio"})
    List<Comprobante> findByPeriodoAndTipo(String periodo, String tipo);

    @EntityGraph(attributePaths = {"items", "items.codigoServicio"})
    List<Comprobante> findByFechaEmisionBetweenOrderByFechaEmisionAscIdAsc(LocalDate desde, LocalDate hasta);

    List<Comprobante> findByAfiliadoIdOrderByFechaEmisionDesc(Long afiliadoId);

    @Query("select distinct c.periodo from Comprobante c order by c.periodo desc")
    List<String> periodosGenerados();
}
