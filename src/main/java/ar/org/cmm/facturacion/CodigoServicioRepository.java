package ar.org.cmm.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodigoServicioRepository extends JpaRepository<CodigoServicio, Long> {

    Optional<CodigoServicio> findByCodigo(String codigo);

    List<CodigoServicio> findAllByOrderByCodigoAsc();

    List<CodigoServicio> findByIncluirEnCuotaTrueAndActivoTrue();

    Optional<CodigoServicio> findFirstByTipoAndActivoTrue(String tipo);
}
