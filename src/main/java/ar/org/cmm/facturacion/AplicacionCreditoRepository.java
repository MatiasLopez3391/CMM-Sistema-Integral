package ar.org.cmm.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AplicacionCreditoRepository extends JpaRepository<AplicacionCredito, Long> {

    List<AplicacionCredito> findByComprobanteId(Long comprobanteId);

    List<AplicacionCredito> findByNotaCreditoIdIn(Collection<Long> notaIds);
}
