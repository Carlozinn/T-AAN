package mx.uv.taan.reportes.infraestructura;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import mx.uv.taan.reportes.aplicacion.FiltroBusqueda;

/**
 * Búsqueda de la bandeja institucional (CU-05; RF-018, RF-024, RF-041) en SQL nativo: los filtros
 * son opcionales y combinables, y el del semáforo se calcula en la base cruzando cada reporte con
 * el tiempo objetivo de su estado. Devuelve solo los ids de la página y el total.
 *
 * <p>Todos los valores van como parámetros; el texto libre escapa % y _ para que se busque tal cual.</p>
 */
@Repository
public class BusquedaReportes {

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private final NamedParameterJdbcTemplate jdbc;

    public BusquedaReportes(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record PaginaIds(List<Long> ids, long total) {
    }

    public PaginaIds buscar(FiltroBusqueda f, int pagina, int tamano) {
        MapSqlParameterSource p = new MapSqlParameterSource("org", f.organizacionId());
        StringBuilder where = new StringBuilder("""
                FROM reporte r
                LEFT JOIN tiempo_objetivo t ON t.organizacion_id = r.organizacion_id AND t.estado = r.estado
                WHERE r.organizacion_id = :org""");
        if (f.areaAlcance() != null) {
            where.append(" AND r.area_id = :areaAlcance");
            p.addValue("areaAlcance", f.areaAlcance());
        }
        if (f.estados() != null && !f.estados().isEmpty()) {
            where.append(" AND r.estado::text IN (:estados)");
            p.addValue("estados", f.estados());
        }
        if (f.areaId() != null) {
            where.append(" AND r.area_id = :areaId");
            p.addValue("areaId", f.areaId());
        }
        if (f.categoriaId() != null) {
            // Una categoría raíz incluye a sus subcategorías.
            where.append("""
                     AND COALESCE(r.categoria_final_id, r.categoria_sugerida_id) IN
                         (SELECT id FROM categoria WHERE id = :cat OR categoria_padre_id = :cat)""");
            p.addValue("cat", f.categoriaId());
        }
        if (f.desde() != null) {
            where.append(" AND r.creado_en >= :desde");
            p.addValue("desde", inicioDelDia(f.desde()));
        }
        if (f.hasta() != null) {
            where.append(" AND r.creado_en < :hasta");
            p.addValue("hasta", inicioDelDia(f.hasta().plusDays(1)));
        }
        if (f.texto() != null && !f.texto().isBlank()) {
            where.append(" AND (r.folio ILIKE :texto OR r.descripcion ILIKE :texto)");
            p.addValue("texto", "%" + escaparLike(f.texto().strip()) + "%");
        }
        if (f.semaforo() != null) {
            where.append(switch (f.semaforo()) {
                case "rojo" -> " AND t.horas_rojo IS NOT NULL AND now() - r.fecha_ultimo_estado >= make_interval(hours => t.horas_rojo)";
                case "amarillo" -> " AND t.horas_amarillo IS NOT NULL"
                        + " AND now() - r.fecha_ultimo_estado >= make_interval(hours => t.horas_amarillo)"
                        + " AND now() - r.fecha_ultimo_estado < make_interval(hours => t.horas_rojo)";
                case "verde" -> " AND t.horas_amarillo IS NOT NULL AND now() - r.fecha_ultimo_estado < make_interval(hours => t.horas_amarillo)";
                default -> " AND t.horas_amarillo IS NULL";
            });
        }
        Long total = jdbc.queryForObject("SELECT count(*) " + where, p, Long.class);
        p.addValue("limite", tamano).addValue("desplazamiento", (long) pagina * tamano);
        List<Long> ids = jdbc.queryForList("SELECT r.id " + where
                + " ORDER BY r.creado_en DESC, r.id DESC LIMIT :limite OFFSET :desplazamiento", p, Long.class);
        return new PaginaIds(ids, total == null ? 0 : total);
    }

    private static java.sql.Timestamp inicioDelDia(LocalDate dia) {
        ZonedDateTime inicio = dia.atStartOfDay(ZONA);
        return java.sql.Timestamp.from(inicio.toInstant());
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
