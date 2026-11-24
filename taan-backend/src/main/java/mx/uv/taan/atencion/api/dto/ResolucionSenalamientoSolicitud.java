package mx.uv.taan.atencion.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Esquema ResolucionSenalamiento del contrato (CU-26 paso 3). diasSuspension = null: indefinida. */
public record ResolucionSenalamientoSolicitud(
        @NotNull Decision decision,
        @NotBlank @Size(min = 10, max = 2000) String motivo,
        @Min(1) @Max(3650) Integer diasSuspension) {

    public enum Decision {
        desestimar,
        suspender
    }
}
