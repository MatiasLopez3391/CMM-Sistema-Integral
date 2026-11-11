package ar.org.cmm.cobranza;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedioCobroRepository extends JpaRepository<MedioCobro, Long> {

    Optional<MedioCobro> findByCodigo(String codigo);

    List<MedioCobro> findByActivoTrueOrderByNombreAsc();
}
