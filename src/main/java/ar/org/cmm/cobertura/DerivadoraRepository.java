package ar.org.cmm.cobertura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DerivadoraRepository extends JpaRepository<Derivadora, Long> {

    List<Derivadora> findAllByOrderByNombreAsc();
}
