package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquema SolicitudLevantamiento del contrato: por qué se levanta la suspensión. */
public record LevantamientoSolicitud(@NotBlank @Size(min = 10, max = 2000) String motivo) {
}
