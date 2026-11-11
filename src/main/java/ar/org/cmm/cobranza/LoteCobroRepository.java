package ar.org.cmm.cobranza;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoteCobroRepository extends JpaRepository<LoteCobro, Long> {

    Optional<LoteCobro> findFirstByHashArchivoAndEstado(String hashArchivo, String estado);

    List<LoteCobro> findTop50ByOrderByIdDesc();
}
