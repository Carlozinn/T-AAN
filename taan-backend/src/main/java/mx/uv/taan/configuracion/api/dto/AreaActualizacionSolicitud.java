package mx.uv.taan.configuracion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Esquema AreaActualizacion del contrato: campos opcionales (null = sin cambio). Para borrar
 * el teléfono, el correo público o las funciones se envía cadena vacía.
 */
public record AreaActualizacionSolicitud(
        @Size(min = 2, max = 150) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String nombre,
        @Size(max = 20) String telefonoPublico,
        @Email @Size(max = 150) String correoPublico,
        @Size(max = 1000) String funciones,
        Boolean activa) {
}
