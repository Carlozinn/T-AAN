package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Esquema LoginCorreo del contrato (sesión ciudadana e institucional). */
public record LoginCorreoSolicitud(
        @NotBlank @Email String correo,
        @NotBlank String password) {
}
