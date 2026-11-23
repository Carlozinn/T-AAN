package mx.uv.taan.reportes.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.uv.taan.reportes.dominio.HistorialReporte;

/** Historial de los reportes (solo inserciones). Solo lo usan los servicios del módulo reportes. */
public interface HistorialReporteRepository extends JpaRepository<HistorialReporte, Long> {

    List<HistorialReporte> findByReporteIdAndVisibleCiudadanoTrueOrderByCreadoEnAscIdAsc(Long reporteId);

    /** Historial completo para el personal (CU-09), incluidas las notas internas. */
    List<HistorialReporte> findByReporteIdOrderByCreadoEnAscIdAsc(Long reporteId);

    boolean existsByReporteIdAndAccion(Long reporteId, String accion);
}
