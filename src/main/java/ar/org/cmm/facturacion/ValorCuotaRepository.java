package ar.org.cmm.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ValorCuotaRepository extends JpaRepository<ValorCuota, Long> {

    @Query("select v from ValorCuota v where v.planId = :planId and v.vigenteDesde <= :fecha order by v.vigenteDesde desc limit 1")
    Optional<ValorCuota> vigente(@Param("planId") Long planId, @Param("fecha") LocalDate fecha);

    List<ValorCuota> findByPlanIdOrderByVigenteDesdeDesc(Long planId);

    Optional<ValorCuota> findByPlanIdAndVigenteDesde(Long planId, LocalDate vigenteDesde);
}
