package mx.uv.taan.cuentas.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Esquema EliminacionCuenta del contrato: confirmación explícita con la palabra ELIMINAR (RF-012). */
public record EliminacionCuentaSolicitud(
        @NotNull @Pattern(regexp = "ELIMINAR", message = "escribe ELIMINAR para confirmar") String confirmacion) {
}
