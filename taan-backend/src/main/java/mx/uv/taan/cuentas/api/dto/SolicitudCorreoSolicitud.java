package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Esquema SolicitudCorreo del contrato (reenvío de verificación y, después, recuperación). */
public record SolicitudCorreoSolicitud(@NotBlank @Email String correo) {
}
