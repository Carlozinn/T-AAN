package mx.uv.taan.administracion.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Esquema ZonaNueva del contrato. geometria es un GeoJSON Polygon o MultiPolygon en WGS84,
 * con longitud primero; puede ser el límite del Marco Geoestadístico del INEGI.
 */
public record ZonaNuevaSolicitud(
        @NotBlank @Size(min = 2, max = 150) String nombre,
        @NotNull Long organizacionReceptoraId,
        @NotEmpty Map<String, Object> geometria) {
}
