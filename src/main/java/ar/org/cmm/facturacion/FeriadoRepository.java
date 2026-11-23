package ar.org.cmm.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FeriadoRepository extends JpaRepository<Feriado, LocalDate> {

    List<Feriado> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
}
