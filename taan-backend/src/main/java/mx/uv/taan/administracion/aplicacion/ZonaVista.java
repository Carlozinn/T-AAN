package mx.uv.taan.administracion.aplicacion;

import java.util.List;
import java.util.Map;

/** Esquema Zona del contrato. geometria es GeoJSON (MultiPolygon, WGS84, longitud primero). */
public record ZonaVista(
        long id,
        String nombre,
        long organizacionReceptoraId,
        List<Long> organizacionesConCobertura,
        boolean activa,
        Map<String, Object> geometria) {
}
