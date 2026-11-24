package mx.uv.taan.atencion.infraestructura;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import mx.uv.taan.atencion.dominio.EstadoSenalamiento;
import mx.uv.taan.atencion.dominio.Senalamiento;

/** Acceso a los señalamientos. Solo lo usan los servicios del módulo atencion. */
public interface SenalamientoRepository extends JpaRepository<Senalamiento, Long> {

    boolean existsByReporteIdAndEstado(Long reporteId, EstadoSenalamiento estado);

    /** Usa ix_senalamiento_estado; los más antiguos primero para revisarlos en orden de llegada. */
    Page<Senalamiento> findByEstadoOrderByCreadoEnAscIdAsc(EstadoSenalamiento estado, Pageable pagina);

    /** Dos administradores no pueden resolver el mismo señalamiento a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Senalamiento s WHERE s.id = :id")
    Optional<Senalamiento> bloquear(Long id);

    long countByCiudadanoIdAndEstado(Long ciudadanoId, EstadoSenalamiento estado);

    /**
     * Antecedentes de varias cuentas en una consulta: [ciudadanoId, total, con el estado dado].
     * El estado va como parámetro: así Hibernate lo envía con el tipo enumerado de la columna.
     */
    @Query("""
            SELECT s.ciudadanoId, count(s), sum(CASE WHEN s.estado = :estado THEN 1L ELSE 0L END)
            FROM Senalamiento s WHERE s.ciudadanoId IN :ids GROUP BY s.ciudadanoId""")
    List<Object[]> antecedentes(Collection<Long> ids, EstadoSenalamiento estado);
}
