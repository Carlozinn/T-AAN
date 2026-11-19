package mx.uv.taan.reportes.aplicacion;

import java.time.Instant;
import java.util.List;

import mx.uv.taan.administracion.aplicacion.CanalExternoVista;
import mx.uv.taan.archivos.aplicacion.ArchivoRef;
import mx.uv.taan.reportes.dominio.MotivoRechazo;

/**
 * Esquema ReporteDetalleCiudadano del contrato (CU-02, CU-03). Los bloques rechazo,
 * reportePrincipal, canalesExternos y reapertura solo vienen llenos en el estado que les
 * corresponde; en los demás llegan como null. Nunca incluye datos del personal.
 */
public record ReporteDetalleCiudadano(
        String folio,
        String estado,
        String estadoTexto,
        String categoria,
        Instant creadoEn,
        Instant actualizadoEn,
        String descripcion,
        Ubicacion ubicacion,
        List<ArchivoRef> fotos,
        String organizacion,
        String area,
        List<EventoPublico> historial,
        Rechazo rechazo,
        ReportePrincipal reportePrincipal,
        List<CanalExternoVista> canalesExternos,
        Reapertura reapertura) {

    public record Ubicacion(double latitud, double longitud) {
    }

    /** Estado rechazado: motivo público y contacto oficial del área (RF-013). */
    public record Rechazo(MotivoRechazo motivo, String texto, ContactoArea contactoArea) {
    }

    public record ContactoArea(String telefono, String correo) {
    }

    /** Estado duplicado: el ciudadano sigue el avance del reporte principal. */
    public record ReportePrincipal(String folio, String estado, String estadoTexto, List<EventoPublico> historial) {
    }

    /** Estado resuelto: si todavía puede reabrirse y hasta cuándo (RF-042). */
    public record Reapertura(boolean permitida, Instant venceEn) {
    }
}
