package mx.uv.taan.configuracion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema AreaNueva del contrato. funciones es opcional (CU-22 paso 3). */
public record AreaNuevaSolicitud(
        @NotBlank @Size(min = 2, max = 150) String nombre,
        @Size(max = 20) String telefonoPublico,
        @Email @Size(max = 150) String correoPublico,
        @Size(max = 1000) String funciones) {
}
