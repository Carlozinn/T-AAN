package mx.uv.taan.configuracion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema AreaNueva del contrato. */
public record AreaNuevaSolicitud(
        @NotBlank @Size(min = 2, max = 150) String nombre,
        @Size(max = 20) String telefonoPublico,
        @Email @Size(max = 150) String correoPublico) {
}
