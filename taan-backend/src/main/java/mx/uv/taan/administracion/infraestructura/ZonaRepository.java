package mx.uv.taan.administracion.infraestructura;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Zonas de cobertura con PostGIS (tablas zona_cobertura y zona_organizacion; RF-029, CU-15).
 *
 * <p>Se usa JDBC en lugar de JPA porque todo el trabajo geográfico lo hace PostGIS: convertir
 * desde y hacia GeoJSON (ST_GeomFromGeoJSON / ST_AsGeoJSON), validar la geometría (ST_IsValid)
 * y saber si un punto cae dentro (ST_Contains, con el índice GIST). Así Java nunca manipula
 * polígonos y no hace falta una librería de GeoJSON.</p>
 */
@Repository
public class ZonaRepository {

    /** Geometría siempre como MultiPolygon en WGS84 (el tipo de la columna). */
    private static final String DESDE_GEOJSON = "ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(?::text), 4326))";

    private static final RowMapper<FilaZona> FILA = (rs, n) -> new FilaZona(
            rs.getLong("id"), rs.getString("nombre"), rs.getLong("organizacion_receptora_id"),
            rs.getBoolean("activa"), rs.getString("geojson"));

    private final JdbcTemplate jdbc;

    public ZonaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Datos de una zona; geoJson es el texto que produce PostGIS (6 decimales ≈ 10 cm). */
    public record FilaZona(long id, String nombre, long organizacionReceptoraId, boolean activa, String geoJson) {
    }

    public List<FilaZona> listar() {
        return jdbc.query("""
                SELECT id, nombre, organizacion_receptora_id, activa, ST_AsGeoJSON(geometria, 6) AS geojson
                FROM zona_cobertura ORDER BY nombre""", FILA);
    }

    public Optional<FilaZona> buscar(long id) {
        return jdbc.query("""
                SELECT id, nombre, organizacion_receptora_id, activa, ST_AsGeoJSON(geometria, 6) AS geojson
                FROM zona_cobertura WHERE id = ?""", FILA, id).stream().findFirst();
    }

    public boolean existe(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM zona_cobertura WHERE id = ?)", Boolean.class, id));
    }

    /**
     * true si el GeoJSON es un Polygon o MultiPolygon bien formado y geométricamente válido
     * (sin autointersecciones ni anillos abiertos). Cualquier error de PostGIS cuenta como inválido.
     */
    public boolean geometriaValida(String geoJson) {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT GeometryType(g) IN ('POLYGON', 'MULTIPOLYGON') AND ST_IsValid(g) AND NOT ST_IsEmpty(g)
                    FROM (SELECT ST_GeomFromGeoJSON(?::text) AS g) AS entrada""", Boolean.class, geoJson));
        } catch (DataAccessException e) {
            return false;
        }
    }

    public long insertar(String nombre, String geoJson, long organizacionReceptoraId) {
        Long id = jdbc.queryForObject("""
                INSERT INTO zona_cobertura (nombre, geometria, organizacion_receptora_id)
                VALUES (?, %s, ?) RETURNING id""".formatted(DESDE_GEOJSON),
                Long.class, nombre, geoJson, organizacionReceptoraId);
        return id;
    }

    /** Cambia solo los campos no nulos. */
    public void actualizar(long id, String nombre, Long organizacionReceptoraId, String geoJson, Boolean activa) {
        jdbc.update("""
                UPDATE zona_cobertura SET
                  nombre = COALESCE(?::varchar, nombre),
                  organizacion_receptora_id = COALESCE(?::bigint, organizacion_receptora_id),
                  geometria = COALESCE(%s, geometria),
                  activa = COALESCE(?::boolean, activa)
                WHERE id = ?""".formatted(DESDE_GEOJSON),
                nombre, organizacionReceptoraId, geoJson, activa, id);
    }

    /** Organizaciones con cobertura de cada zona. */
    public Map<Long, List<Long>> organizacionesPorZona() {
        Map<Long, List<Long>> resultado = new LinkedHashMap<>();
        jdbc.query("SELECT zona_id, organizacion_id FROM zona_organizacion ORDER BY zona_id, organizacion_id",
                fila -> {
                    resultado.computeIfAbsent(fila.getLong("zona_id"), z -> new ArrayList<>())
                            .add(fila.getLong("organizacion_id"));
                });
        return resultado;
    }

    public List<Long> organizacionesDe(long zonaId) {
        return jdbc.queryForList("SELECT organizacion_id FROM zona_organizacion WHERE zona_id = ? ORDER BY 1",
                Long.class, zonaId);
    }

    public void agregarOrganizacion(long zonaId, long organizacionId) {
        jdbc.update("""
                INSERT INTO zona_organizacion (zona_id, organizacion_id) VALUES (?, ?)
                ON CONFLICT DO NOTHING""", zonaId, organizacionId);
    }

    public void reemplazarOrganizaciones(long zonaId, Collection<Long> organizacionIds) {
        jdbc.update("DELETE FROM zona_organizacion WHERE zona_id = ?", zonaId);
        organizacionIds.forEach(org -> agregarOrganizacion(zonaId, org));
    }

    public boolean tieneCobertura(long zonaId, long organizacionId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM zona_organizacion WHERE zona_id = ? AND organizacion_id = ?)",
                Boolean.class, zonaId, organizacionId));
    }

    /** Zonas activas donde la organización es la receptora por defecto: su propio territorio. */
    public List<Long> zonasActivasConReceptora(long organizacionId) {
        return jdbc.queryForList("""
                SELECT id FROM zona_cobertura WHERE activa AND organizacion_receptora_id = ? ORDER BY id""",
                Long.class, organizacionId);
    }

    /**
     * Zona activa que contiene el punto (la de menor superficie si hay varias, CU-15 FA-01),
     * sin la geometría: la usa el motor de reglas.
     */
    public Optional<FilaZona> zonaActivaQueContiene(double latitud, double longitud) {
        return jdbc.query("""
                SELECT id, nombre, organizacion_receptora_id, activa, NULL AS geojson
                FROM zona_cobertura
                WHERE activa AND ST_Contains(geometria, ST_SetSRID(ST_MakePoint(?, ?), 4326))
                ORDER BY ST_Area(geometria) LIMIT 1""", FILA, longitud, latitud)
                .stream().findFirst();
    }

    /** Organización activa con cobertura; nivel es el valor de nivel_participacion. */
    public record FilaOrganizacion(long id, String nombre, String nivel) {
    }

    /**
     * Organizaciones activas con cobertura en cualquier zona activa que contenga el punto: los
     * destinos posibles de una redirección (CU-21, RF-037).
     */
    public List<FilaOrganizacion> organizacionesActivasConCobertura(double latitud, double longitud) {
        return jdbc.query("""
                SELECT DISTINCT o.id, o.nombre, o.nivel_participacion::text AS nivel
                FROM zona_cobertura z
                JOIN zona_organizacion zo ON zo.zona_id = z.id
                JOIN organizacion o ON o.id = zo.organizacion_id
                WHERE z.activa AND o.activa AND ST_Contains(z.geometria, ST_SetSRID(ST_MakePoint(?, ?), 4326))
                ORDER BY o.nombre""",
                (rs, n) -> new FilaOrganizacion(rs.getLong("id"), rs.getString("nombre"), rs.getString("nivel")),
                longitud, latitud);
    }

    /**
     * Nombre de la zona activa que contiene el punto. Si hay zonas superpuestas se toma la de
     * menor superficie, por ser la más específica (CU-15 FA-01).
     */
    public Optional<String> zonaQueContiene(double latitud, double longitud) {
        return jdbc.queryForList("""
                SELECT nombre FROM zona_cobertura
                WHERE activa AND ST_Contains(geometria, ST_SetSRID(ST_MakePoint(?, ?), 4326))
                ORDER BY ST_Area(geometria) LIMIT 1""", String.class, longitud, latitud)
                .stream().findFirst();
    }
}
