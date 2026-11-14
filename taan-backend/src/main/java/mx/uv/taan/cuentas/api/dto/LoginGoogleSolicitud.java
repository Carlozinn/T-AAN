package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Esquema LoginGoogle del contrato. captchaToken y aceptaAvisoPrivacidad solo son
 * obligatorios si el correo aún no tiene cuenta; esa regla la aplica el servicio.
 */
public record LoginGoogleSolicitud(
        @NotBlank String idToken,
        String captchaToken,
        Boolean aceptaAvisoPrivacidad) {
}
