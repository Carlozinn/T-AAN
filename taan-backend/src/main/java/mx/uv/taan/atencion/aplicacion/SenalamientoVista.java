package mx.uv.taan.atencion.aplicacion;

import java.time.Instant;

import mx.uv.taan.atencion.dominio.EstadoSenalamiento;

/**
 * Esquema Senalamiento del contrato (CU-26 paso 2). ciudadano es null si la cuenta se eliminó.
 * Los antecedentes no cuentan este mismo señalamiento.
 */
public record SenalamientoVista(long id, String folio, String descripcion, EventoHistorial.UsuarioRef ciudadano,
                                String organizacion, String senaladoPor, String motivo, EstadoSenalamiento estado,
                                int senalamientosPrevios, int suspensionesPrevias, Instant creadoEn) {
}
