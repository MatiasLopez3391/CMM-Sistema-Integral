package ar.org.cmm.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuentaContableRepository extends JpaRepository<CuentaContable, Long> {

    Optional<CuentaContable> findByCodigo(String codigo);

    List<CuentaContable> findAllByOrderByCodigoAsc();
}
