package ar.org.cmm.facturacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IndiceIpcRepository extends JpaRepository<IndiceIpc, String> {

    List<IndiceIpc> findTop24ByOrderByPeriodoDesc();
}
