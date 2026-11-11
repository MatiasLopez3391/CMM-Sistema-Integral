package ar.org.cmm.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ImputacionRepository extends JpaRepository<Imputacion, Long> {

    List<Imputacion> findByComprobanteId(Long comprobanteId);

    List<Imputacion> findByPagoIdIn(Collection<Long> pagoIds);
}
