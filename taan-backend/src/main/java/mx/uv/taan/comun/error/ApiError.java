package mx.uv.taan.comun.error;

import java.util.List;

/** Cuerpo de error común a toda la API (esquema "Error" de TAAN_API.yaml). */
public record ApiError(String codigo, String mensaje, List<DetalleCampo> detalles) {

    /** Error de validación de un campo concreto. */
    public record DetalleCampo(String campo, String problema) {
    }
}
