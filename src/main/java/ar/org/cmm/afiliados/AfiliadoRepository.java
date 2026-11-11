package ar.org.cmm.afiliados;

import ar.org.cmm.afiliados.Afiliado.EstadoAfiliado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AfiliadoRepository extends JpaRepository<Afiliado, Long> {

    @EntityGraph(attributePaths = {"plan", "titular"})
    Optional<Afiliado> findByNroAfiliado(String nroAfiliado);

    @EntityGraph(attributePaths = {"plan", "area", "sector", "derivadora", "titular"})
    Optional<Afiliado> findWithDetalleById(Long id);

    boolean existsByTipoDocumentoAndDocumento(String tipoDocumento, String documento);

    Optional<Afiliado> findByTipoDocumentoAndDocumento(String tipoDocumento, String documento);

    /**
     * Búsqueda por número de afiliado, documento, apellido o nombre. Filtros opcionales.
     */
    @EntityGraph(attributePaths = {"plan", "titular"})
    @Query("""
            select a from Afiliado a left join a.titular t
            where (:texto is null
                   or a.nroAfiliado like concat(:texto, '%')
                   or a.documento like concat(:texto, '%')
                   or lower(a.apellido) like lower(concat('%', :texto, '%'))
                   or lower(a.nombre) like lower(concat('%', :texto, '%')))
              and (:estado is null or a.estado = :estado)
              and (:planId is null or a.plan.id = :planId)
              and (:soloTitulares = false or a.titular is null)
              and (:bloqueado is null or coalesce(t.bloqueado, a.bloqueado) = :bloqueado)
            order by a.apellido, a.nombre""")
    Page<Afiliado> buscar(@Param("texto") String texto,
                          @Param("estado") EstadoAfiliado estado,
                          @Param("planId") Long planId,
                          @Param("soloTitulares") boolean soloTitulares,
                          @Param("bloqueado") Boolean bloqueado,
                          Pageable pageable);

    @EntityGraph(attributePaths = {"plan"})
    @Query("select a from Afiliado a left join a.titular t where (a.id = :titularId or t.id = :titularId) order by a.nroAfiliado")
    List<Afiliado> grupoFamiliar(@Param("titularId") Long titularId);

    @EntityGraph(attributePaths = {"plan"})
    List<Afiliado> findByTitularIsNullAndEstadoOrderByNroAfiliadoAsc(EstadoAfiliado estado);

    default List<Afiliado> titularesActivos() {
        return findByTitularIsNullAndEstadoOrderByNroAfiliadoAsc(EstadoAfiliado.ACTIVO);
    }

    @Query("select max(a.nroAfiliado) from Afiliado a where a.nroAfiliado like concat(:base, '%')")
    String ultimoNroDelGrupo(@Param("base") String base);
}
