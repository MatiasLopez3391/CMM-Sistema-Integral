package ar.org.cmm.cobertura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PracticaRepository extends JpaRepository<Practica, Long> {

    Optional<Practica> findByCodigoCmm(String codigoCmm);

    List<Practica> findAllByOrderByCodigoCmmAsc();

    @Query("""
            select p from Practica p
            where lower(p.codigoCmm) like lower(concat('%', :texto, '%'))
               or lower(p.descripcion) like lower(concat('%', :texto, '%'))
            order by p.codigoCmm""")
    List<Practica> buscar(@Param("texto") String texto);
}
