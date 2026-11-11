package ar.org.cmm.cobertura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PracticaRepository extends JpaRepository<Practica, Long> {

    Optional<Practica> findByCodigoCmm(String codigoCmm);

    List<Practica> findAllByOrderByCodigoCmmAsc();
}
