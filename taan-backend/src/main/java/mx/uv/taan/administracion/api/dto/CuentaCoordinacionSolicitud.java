package mx.uv.taan.administracion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema CuentaCoordinacionNueva del contrato. */
public record CuentaCoordinacionSolicitud(
        @NotBlank @Size(min = 2, max = 150) String nombre,
        @NotBlank @Email @Size(max = 150) String correo) {
}
