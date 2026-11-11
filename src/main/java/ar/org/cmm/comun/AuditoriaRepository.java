package ar.org.cmm.comun;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<AuditoriaCambio, Long> {

    List<AuditoriaCambio> findByEntidadAndEntidadId(String entidad, Long entidadId, Pageable page);
}
