package mx.uv.taan.reportes.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.uv.taan.reportes.dominio.OrigenReporte;

/**
 * Esquema DatosNuevoReporte del contrato: la parte «datos» (JSON) del multipart. campoAuxiliar
 * es el honeypot: no se valida aquí para poder registrar el intento como abuso (RNF-022).
 */
public record DatosNuevoReporte(
        @NotBlank @Size(min = 15, max = 2000) String descripcion,
        @NotNull @Valid Ubicacion ubicacion,
        @NotNull OrigenReporte origen,
        String campoAuxiliar) {

    public record Ubicacion(
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud) {
    }
}
