package mx.uv.taan.administracion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.uv.taan.administracion.dominio.TipoCanal;

/** Esquema CanalExternoNuevo del contrato. */
public record CanalExternoNuevoSolicitud(
        @NotNull Long organizacionId,
        @NotNull TipoCanal tipo,
        @NotBlank @Size(max = 255) String valor,
        @NotBlank @Size(max = 255) String fuenteVerificacion) {
}
