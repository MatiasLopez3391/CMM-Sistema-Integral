package ar.org.cmm.historiaclinica;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinicaEntrada, Long> {

    List<HistoriaClinicaEntrada> findByAfiliadoIdInOrderByFechaDescIdDesc(Collection<Long> afiliadoIds);
}
