package mx.uv.taan.reportes.infraestructura;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import mx.uv.taan.reportes.aplicacion.FiltroIndicadores;
import mx.uv.taan.reportes.aplicacion.IndicadoresReportes;
import mx.uv.taan.reportes.aplicacion.IndicadoresReportes.Conteo;

/**
 * Indicadores de CU-10 en SQL nativo: todo se agrega en la base (GROUP BY y FILTER), sin cargar
 * reportes en memoria. Seis consultas por petición, cada una acotada por organización y periodo
 * (ix_reporte_org_creado). El semáforo se cruza con tiempo_objetivo como en la bandeja: solo los
 * estados abiertos tienen tiempo configurado.
 */
@Repository
public class EstadisticasReportes {

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    private static final RowMapper<Conteo> CONTEO = (rs, i) -> new Conteo(rs.getString("etiqueta"), rs.getLong("total"));

    private final NamedParameterJdbcTemplate jdbc;

    public EstadisticasReportes(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public IndicadoresReportes calcular(FiltroIndicadores f) {
        MapSqlParameterSource p = new MapSqlParameterSource("org", f.organizacionId())
                .addValue("desde", inicioDelDia(f.desde()))
                .addValue("hasta", inicioDelDia(f.hasta().plusDays(1)));
        String ambito = " r.organizacion_id = :org AND r.creado_en >= :desde AND r.creado_en < :hasta";
        if (f.areaId() != null) {
            ambito += " AND r.area_id = :area";
            p.addValue("area", f.areaId());
        }

        List<Conteo> porEstado = jdbc.query("""
                SELECT r.estado::text AS etiqueta, count(*) AS total FROM reporte r
                WHERE %s GROUP BY r.estado ORDER BY total DESC, etiqueta""".formatted(ambito), p, CONTEO);
        List<Conteo> porCategoria = jdbc.query("""
                SELECT COALESCE(c.nombre, 'Sin categoría') AS etiqueta, count(*) AS total FROM reporte r
                LEFT JOIN categoria c ON c.id = COALESCE(r.categoria_final_id, r.categoria_sugerida_id)
                WHERE %s GROUP BY c.id, c.nombre ORDER BY total DESC, etiqueta""".formatted(ambito), p, CONTEO);
        List<Conteo> porArea = jdbc.query("""
                SELECT COALESCE(a.nombre, 'Sin área') AS etiqueta, count(*) AS total FROM reporte r
                LEFT JOIN area a ON a.id = r.area_id
                WHERE %s GROUP BY a.id, a.nombre ORDER BY total DESC, etiqueta""".formatted(ambito), p, CONTEO);
        // Un reporte cuenta en cada zona de la organización que lo contiene (las zonas pueden traslaparse).
        List<Conteo> porZona = jdbc.query("""
                SELECT z.nombre AS etiqueta, count(*) AS total FROM reporte r
                JOIN zona_organizacion zo ON zo.organizacion_id = :org
                JOIN zona_cobertura z ON z.id = zo.zona_id AND ST_Contains(z.geometria, r.ubicacion)
                WHERE %s GROUP BY z.id, z.nombre ORDER BY total DESC, etiqueta""".formatted(ambito), p, CONTEO);

        long redirigidos = redirigidos(f, p);
        return jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE r.reabierto) AS reabiertos,
                       (avg(EXTRACT(EPOCH FROM (asig.primera - r.creado_en))) / 3600)::float8 AS horas_asignacion,
                       (avg(EXTRACT(EPOCH FROM (r.fecha_resolucion - r.creado_en))) / 3600)::float8 AS horas_resolucion,
                       count(*) FILTER (WHERE t.horas_rojo IS NOT NULL
                           AND now() - r.fecha_ultimo_estado >= make_interval(hours => t.horas_rojo)) AS atrasados,
                       count(*) FILTER (WHERE t.horas_rojo IS NOT NULL
                           AND now() - r.fecha_ultimo_estado < make_interval(hours => t.horas_rojo)) AS en_tiempo
                FROM reporte r
                LEFT JOIN tiempo_objetivo t ON t.organizacion_id = r.organizacion_id AND t.estado = r.estado
                LEFT JOIN LATERAL (SELECT min(h.creado_en) AS primera FROM historial_reporte h
                                   WHERE h.reporte_id = r.id AND h.estado_nuevo = 'asignado') asig ON true
                WHERE %s""".formatted(ambito), p, (rs, i) -> new IndicadoresReportes(porEstado, porCategoria,
                porArea, porZona, rs.getObject("horas_asignacion", Double.class),
                rs.getObject("horas_resolucion", Double.class), rs.getLong("en_tiempo"), rs.getLong("atrasados"),
                rs.getLong("reabiertos"), redirigidos));
    }

    /** Los redirigidos ya pertenecen a otra organización: se cuentan desde el historial de quien los envió. */
    private long redirigidos(FiltroIndicadores f, MapSqlParameterSource p) {
        Long total = jdbc.queryForObject("""
                SELECT count(DISTINCT h.reporte_id) FROM historial_reporte h
                JOIN reporte r ON r.id = h.reporte_id
                WHERE h.accion = 'redireccion' AND h.organizacion_anterior_id = :org
                  AND r.creado_en >= :desde AND r.creado_en < :hasta"""
                + (f.areaId() == null ? "" : " AND h.area_anterior_id = :area"), p, Long.class);
        return total == null ? 0 : total;
    }

    private static Timestamp inicioDelDia(LocalDate dia) {
        return Timestamp.from(dia.atStartOfDay(ZONA).toInstant());
    }
}
