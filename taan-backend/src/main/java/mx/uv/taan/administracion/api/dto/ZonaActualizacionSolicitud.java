package mx.uv.taan.administracion.api.dto;

import java.util.Map;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Esquema ZonaActualizacion del contrato: campos opcionales (null = sin cambio). */
public record ZonaActualizacionSolicitud(
        @Size(min = 2, max = 150) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String nombre,
        Long organizacionReceptoraId,
        Map<String, Object> geometria,
        Boolean activa) {
}
