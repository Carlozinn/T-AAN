package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Esquema RegistroCorreo del contrato. 72 es el máximo que admite BCrypt. */
public record RegistroCorreoSolicitud(
        @NotBlank @Size(min = 2, max = 150) String nombre,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull @AssertTrue(message = "debe aceptarse") Boolean aceptaAvisoPrivacidad,
        @NotBlank String captchaToken,
        String campoAuxiliar) {
}
