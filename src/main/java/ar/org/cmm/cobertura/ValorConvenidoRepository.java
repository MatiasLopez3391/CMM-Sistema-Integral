package ar.org.cmm.cobertura;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ValorConvenidoRepository extends JpaRepository<ValorConvenido, Long> {

    /** Historial completo de un prestador (más reciente primero). */
    @Query("""
            select v from ValorConvenido v join fetch v.practica
            where v.prestador.id = :prestadorId
            order by v.practica.codigoCmm, v.vigenteDesde desc""")
    List<ValorConvenido> historial(@Param("prestadorId") Long prestadorId);

    /** Valor vigente a una fecha: la vigencia más reciente que no sea posterior a la fecha. */
    @Query("""
            select v from ValorConvenido v
            where v.prestador.id = :prestadorId and v.practica.id = :practicaId and v.vigenteDesde <= :fecha
            order by v.vigenteDesde desc limit 1""")
    Optional<ValorConvenido> vigente(@Param("prestadorId") Long prestadorId,
                                     @Param("practicaId") Long practicaId,
                                     @Param("fecha") LocalDate fecha);

    Optional<ValorConvenido> findByPrestadorIdAndPracticaIdAndVigenteDesde(Long prestadorId, Long practicaId, LocalDate vigenteDesde);
}
