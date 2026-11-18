package mx.uv.taan.administracion.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.uv.taan.administracion.dominio.TipoCanal;

/** Esquema CanalExternoActualizacion del contrato: campos opcionales (null = sin cambio). */
public record CanalExternoActualizacionSolicitud(
        TipoCanal tipo,
        @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String valor,
        @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String fuenteVerificacion,
        Boolean activo) {
}
