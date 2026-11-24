package mx.uv.taan.reportes.aplicacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.OrigenReporte;
import mx.uv.taan.reportes.dominio.Reporte;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;

class PublicadorAvisosTest {

    private static final String FOLIO = "TAAN-2026-000001";

    private ApplicationEventPublisher eventos;
    private ServicioOrganizaciones organizaciones;
    private PublicadorAvisos publicador;
    private Reporte reporte;

    @BeforeEach
    void preparar() {
        eventos = mock(ApplicationEventPublisher.class);
        organizaciones = mock(ServicioOrganizaciones.class);
        ReporteRepository reportes = mock(ReporteRepository.class);
        publicador = new PublicadorAvisos(eventos, reportes, organizaciones);
        reporte = new Reporte(FOLIO, 7L, "Hay un bache enorme en la calle", 18.85, -97.06, OrigenReporte.app);
        ReflectionTestUtils.setField(reporte, "id", 100L);
        when(reportes.findById(100L)).thenReturn(Optional.of(reporte));
    }

    @Test
    void unCambioDeEstadoVisibleSeAvisaConElTextoDelEstado() {
        publicador.publicar(HistorialReporte.cambioPorPersonal(100L, EstadoReporte.ASIGNADO,
                EstadoReporte.EN_ATENCION, "inicio_atencion", "El área comenzó a atender tu reporte.", 3L));

        verify(eventos).publishEvent(new AvisoCiudadano(7L, FOLIO, "Tu reporte " + FOLIO + " ahora está «En atención»."));
    }

    @Test
    void laCanalizacionAutomaticaTambienSeAvisa() {
        publicador.publicar(HistorialReporte.canalizacion(100L, EstadoReporte.RECIBIDO, 1L, null, "Texto"));

        verify(eventos).publishEvent(new AvisoCiudadano(7L, FOLIO, "Tu reporte " + FOLIO + " ahora está «Recibido»."));
    }

    @Test
    void laRedireccionSeAvisaConLaOrganizacionQueAhoraLoAtiende() {
        when(organizaciones.nombres(List.of(9L))).thenReturn(Map.of(9L, "Ayuntamiento de Orizaba"));

        publicador.publicar(HistorialReporte.redireccion(100L, EstadoReporte.RECIBIDO, 1L, 9L, null, "Texto", 3L));

        verify(eventos).publishEvent(new AvisoCiudadano(7L, FOLIO,
                "Tu reporte " + FOLIO + " ahora lo atiende Ayuntamiento de Orizaba."));
    }

    @Test
    void laReasignacionDeAreaNoSeAvisa() {
        publicador.publicar(HistorialReporte.cambioDeArea(100L, EstadoReporte.ASIGNADO, "reasignacion", 4L, 5L,
                "Tu reporte pasó al área Bacheo.", 3L));

        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void loQueHaceElCiudadanoYLasNotasInternasNoSeAvisan() {
        publicador.publicar(HistorialReporte.registro(100L));
        publicador.publicar(HistorialReporte.reapertura(100L, "Sigue el bache"));
        publicador.publicar(HistorialReporte.confirmacion(100L));
        publicador.publicar(HistorialReporte.nota(100L, "justificacion_rechazo", "Interna", 3L));

        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void unReporteDeUnaCuentaEliminadaNoSeAvisa() {
        ReflectionTestUtils.setField(reporte, "ciudadanoId", null);

        publicador.publicar(HistorialReporte.cambioPorPersonal(100L, EstadoReporte.EN_ATENCION,
                EstadoReporte.RESUELTO, "resolucion", "Resuelto", 3L));

        verify(eventos, never()).publishEvent(any());
    }
}
