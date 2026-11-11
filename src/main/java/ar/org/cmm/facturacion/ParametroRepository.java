package ar.org.cmm.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParametroRepository extends JpaRepository<Parametro, String> {

    List<Parametro> findAllByOrderByClaveAsc();
}
