package ar.org.cmm.cobranza;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByLoteIdOrderByLineaAsc(Long loteId);

    /** ¿Ya se registró un pago con esta referencia para el medio? (evita duplicados entre archivos) */
    boolean existsByMedioCobroIdAndReferenciaAndEstado(Long medioCobroId, String referencia, String estado);

    List<Pago> findByAfiliadoIdAndEstadoOrderByFechaPagoDesc(Long afiliadoId, String estado);

    @Query("select p from Pago p where p.estado = 'IMPUTADO' and p.fechaPago = :fecha order by p.medioCobro.nombre, p.id")
    List<Pago> imputadosDelDia(@Param("fecha") LocalDate fecha);

    List<Pago> findTop100ByLoteIsNullOrderByIdDesc();
}
