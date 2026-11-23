package mx.uv.taan.atencion.aplicacion;

import java.time.Instant;
import java.util.List;

import mx.uv.taan.archivos.aplicacion.ArchivoRef;
import mx.uv.taan.atencion.dominio.Semaforo;
import mx.uv.taan.cuentas.aplicacion.ContactoCiudadano;
import mx.uv.taan.reportes.dominio.MotivoCanalizacion;

/**
 * Esquema ReporteDetalleInstitucional del contrato (CU-05, CU-09).
 *
 * <p>ciudadano (nombre y teléfono) solo llega al personal de la organización responsable
 * (RF-038): es null si la cuenta se eliminó o tras una redirección. evidenciasAtencion son las
 * fotos que sube el personal (CU-18), separadas de las del ciudadano (RF-023, punto 2).</p>
 */
public record ReporteDetalleInstitucional(
        String folio,
        String estado,
        String categoria,
        String area,
        Instant creadoEn,
        Instant fechaUltimoEstado,
        Semaforo semaforo,
        boolean posibleContenidoNoValido,
        int contadorDuplicados,
        boolean reabierto,
        String descripcion,
        Ubicacion ubicacion,
        List<ArchivoRef> fotos,
        List<ArchivoRef> evidenciasAtencion,
        ContactoCiudadano ciudadano,
        String categoriaSugerida,
        String categoriaFinal,
        MotivoCanalizacion motivoCanalizacion,
        String organizacion,
        String reportePrincipalFolio,
        List<EventoHistorial> historial) {

    public record Ubicacion(double latitud, double longitud) {
    }
}
