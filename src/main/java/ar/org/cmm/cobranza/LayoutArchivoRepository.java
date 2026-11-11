package ar.org.cmm.cobranza;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LayoutArchivoRepository extends JpaRepository<LayoutArchivo, Long> {

    List<LayoutArchivo> findAllByOrderByNombreAsc();

    Optional<LayoutArchivo> findFirstByMedioCobroIdAndActivoTrue(Long medioCobroId);
}
