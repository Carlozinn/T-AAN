package mx.uv.taan.reportes.infraestructura;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import mx.uv.taan.reportes.dominio.Reporte;

/** Acceso a los reportes. Solo lo usan los servicios del módulo reportes. */
public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    Page<Reporte> findByCiudadanoIdOrderByCreadoEnDesc(Long ciudadanoId, Pageable pagina);

    Optional<Reporte> findByFolioAndCiudadanoId(String folio, Long ciudadanoId);

    boolean existsByIdAndCiudadanoId(Long id, Long ciudadanoId);

    /**
     * Bloquea la fila (SELECT ... FOR UPDATE) mientras se canaliza: si el procesamiento
     * inmediato y la tarea de reintento coinciden, el segundo espera y ya no lo encuentra pendiente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reporte r WHERE r.id = :id")
    Optional<Reporte> bloquear(Long id);

    /** Igual que {@link #bloquear}, por folio: dos personas no pueden cambiar el mismo reporte a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reporte r WHERE r.folio = :folio")
    Optional<Reporte> bloquearPorFolio(String folio);

    Optional<Reporte> findByFolio(String folio);

    /** «Reportado por N personas» (RF-019): reportes que apuntan a cada principal. */
    @Query("SELECT r.reportePrincipalId, count(r) FROM Reporte r WHERE r.reportePrincipalId IN :ids GROUP BY r.reportePrincipalId")
    List<Object[]> contarDuplicados(Collection<Long> ids);

    /**
     * Pendientes de canalización creados antes de cierto momento (para reintentarlos). Es SQL
     * nativo porque la columna es del tipo enumerado estado_reporte: el literal de texto se
     * convierte solo, mientras que un parámetro de texto no se puede comparar con ella.
     */
    @Query(value = """
            SELECT id FROM reporte
            WHERE estado = 'pendiente_canalizacion' AND creado_en < :antesDe
            ORDER BY creado_en LIMIT 50""", nativeQuery = true)
    List<Long> pendientesCreadosAntesDe(Instant antesDe);
}
