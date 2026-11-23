
package ar.org.cmm.ordenes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByNumero(String numero);

    List<Orden> findByAfiliadoIdOrderByFechaEmisionDesc(Long afiliadoId);

    List<Orden> findByEstadoOrderByFechaEmisionDesc(String estado);

    boolean existsByNumero(String numero);
}
