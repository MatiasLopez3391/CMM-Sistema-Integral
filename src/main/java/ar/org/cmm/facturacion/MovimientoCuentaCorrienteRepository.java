package ar.org.cmm.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MovimientoCuentaCorrienteRepository extends JpaRepository<MovimientoCuentaCorriente, Long> {

    List<MovimientoCuentaCorriente> findByAfiliadoIdOrderByFechaAscIdAsc(Long afiliadoId);

    @Query("""
            select coalesce(sum(case when m.tipo = 'DEBE' then m.importe else -m.importe end), 0)
            from MovimientoCuentaCorriente m where m.afiliadoId = :afiliadoId""")
    BigDecimal saldo(@Param("afiliadoId") Long afiliadoId);

    @Query("""
            select coalesce(sum(case when m.tipo = 'DEBE' then m.importe else -m.importe end), 0)
            from MovimientoCuentaCorriente m where m.afiliadoId = :afiliadoId and m.fecha < :fecha""")
    BigDecimal saldoAntesDe(@Param("afiliadoId") Long afiliadoId, @Param("fecha") LocalDate fecha);

    List<MovimientoCuentaCorriente> findByTipoAndFechaBetweenAndPagoIdIsNotNullOrderByFechaAscIdAsc(
            String tipo, LocalDate desde, LocalDate hasta);
}
