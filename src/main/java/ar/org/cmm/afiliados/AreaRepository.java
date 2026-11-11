package ar.org.cmm.afiliados;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Long> {

    @EntityGraph(attributePaths = "sectores")
    List<Area> findAllByOrderByNombreAsc();
}
