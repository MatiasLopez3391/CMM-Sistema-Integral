
package ar.org.cmm.ordenes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenEstadoHistorialRepository
        extends JpaRepository<OrdenEstadoHistorial, Long> {

    List<OrdenEstadoHistorial> findByOrdenIdOrderByFechaAsc(Long ordenId);
}
