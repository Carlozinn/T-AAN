package mx.uv.taan.reportes.aplicacion;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.Reporte;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;

/**
 * Decide qué eventos del historial se avisan al ciudadano y publica el {@link AvisoCiudadano}.
 *
 * <p>Se avisa cada cambio de estado visible para el ciudadano y la redirección a otra organización.
 * No se avisa lo que hace el propio ciudadano (registro, reapertura), la reasignación de área (el
 * estado no cambia) ni a un reporte cuya cuenta se eliminó.</p>
 */
@Component
class PublicadorAvisos {

    private static final List<String> ACCIONES_DEL_CIUDADANO = List.of("registro", "reapertura");

    private final ApplicationEventPublisher eventos;
    private final ReporteRepository reportes;
    private final ServicioOrganizaciones organizaciones;

    PublicadorAvisos(ApplicationEventPublisher eventos, ReporteRepository reportes,
                     ServicioOrganizaciones organizaciones) {
        this.eventos = eventos;
        this.reportes = reportes;
        this.organizaciones = organizaciones;
    }

    /** Debe llamarse dentro de la transacción que guarda el evento; el reporte ya está en el contexto de persistencia. */
    void publicar(HistorialReporte evento) {
        if (!evento.isVisibleCiudadano() || evento.getEstadoNuevo() == null
                || ACCIONES_DEL_CIUDADANO.contains(evento.getAccion())) {
            return;
        }
        boolean redireccion = "redireccion".equals(evento.getAccion());
        if (!redireccion && evento.getEstadoAnterior() == evento.getEstadoNuevo()) {
            return;
        }
        Reporte r = reportes.findById(evento.getReporteId()).orElse(null);
        if (r == null || r.getCiudadanoId() == null) {
            return;
        }
        String resumen = redireccion
                ? "Tu reporte " + r.getFolio() + " ahora lo atiende " + nombreOrganizacion(evento.getOrganizacionNuevaId()) + "."
                : "Tu reporte " + r.getFolio() + " ahora está «" + evento.getEstadoNuevo().textoCiudadano() + "».";
        eventos.publishEvent(new AvisoCiudadano(r.getCiudadanoId(), r.getFolio(), resumen));
    }

    private String nombreOrganizacion(Long organizacionId) {
        return organizaciones.nombres(List.of(organizacionId)).getOrDefault(organizacionId, "otra institución");
    }
}
