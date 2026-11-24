
package ar.org.cmm.ordenes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaMedicaRepository
        extends JpaRepository<AuditoriaMedica, Long> {

    List<AuditoriaMedica> findByOrdenIdOrderByFechaDesc(Long ordenId);
}

