package mx.uv.taan.configuracion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.uv.taan.cuentas.dominio.Rol;

/** Esquema UsuarioNuevo del contrato. areaId es obligatoria si rol = personal_area. */
public record UsuarioNuevoSolicitud(
        @NotBlank @Size(min = 2, max = 150) String nombre,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotNull Rol rol,
        Long areaId) {
}
