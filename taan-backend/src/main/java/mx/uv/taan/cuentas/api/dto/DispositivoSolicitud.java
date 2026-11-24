package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema Dispositivo del contrato: token de Firebase Cloud Messaging. */
public record DispositivoSolicitud(@NotBlank @Size(max = 500) String tokenDispositivo) {
}
