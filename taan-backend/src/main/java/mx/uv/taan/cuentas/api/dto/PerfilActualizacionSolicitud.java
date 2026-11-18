package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Esquema PerfilActualizacion del contrato: todos los campos son opcionales y solo se
 * cambian los enviados. Las validaciones se aplican únicamente a los campos presentes.
 */
public record PerfilActualizacionSolicitud(
        @Size(min = 2, max = 150) @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") String nombre,
        @Pattern(regexp = "^[0-9]{10}$", message = "deben ser 10 dígitos, sin espacios ni guiones") String telefono,
        Boolean avisosCorreo) {
}
