package ar.org.cmm.afiliados;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BloqueoHistorialRepository extends JpaRepository<BloqueoHistorial, Long> {

    List<BloqueoHistorial> findByAfiliadoIdOrderByFechaDesc(Long afiliadoId);
}
