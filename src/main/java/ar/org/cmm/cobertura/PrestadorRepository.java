package ar.org.cmm.cobertura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrestadorRepository extends JpaRepository<Prestador, Long> {

    List<Prestador> findAllByOrderByNombreAsc();
}
