package mx.uv.taan.administracion.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Esquema CategoriaActualizacion del contrato: campos opcionales (null = sin cambio). */
public record CategoriaActualizacionSolicitud(
        @Size(min = 2, max = 150) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String nombre,
        Boolean activa) {
}
