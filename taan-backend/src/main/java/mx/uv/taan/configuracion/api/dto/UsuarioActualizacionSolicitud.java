package mx.uv.taan.configuracion.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.uv.taan.cuentas.dominio.Rol;

/**
 * Esquema UsuarioActualizacion del contrato: campos opcionales (null = sin cambio). Al pasar
 * a coordinacion el área se quita sola; al pasar a personal_area hace falta un área.
 */
public record UsuarioActualizacionSolicitud(
        @Size(min = 2, max = 150) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String nombre,
        Rol rol,
        Long areaId,
        Boolean activo) {
}
