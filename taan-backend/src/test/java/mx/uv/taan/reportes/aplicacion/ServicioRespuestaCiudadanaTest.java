package mx.uv.taan.reportes.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.aplicacion.CondicionCiudadano;
import mx.uv.taan.cuentas.aplicacion.ServicioPerfilCiudadano;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.MotivoCanalizacion;
import mx.uv.taan.reportes.dominio.OrigenReporte;
import mx.uv.taan.reportes.dominio.Reporte;
import mx.uv.taan.reportes.infraestructura.HistorialReporteRepository;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;

class ServicioRespuestaCiudadanaTest {

    private static final long CIUDADANO = 7L;
    private static final String FOLIO = "TAAN-2026-000001";

    private ReporteRepository reportes;
    private HistorialReporteRepository historial;
    private ServicioConsultaCiudadana consulta;
    private ServicioPerfilCiudadano perfiles;
    private ServicioRespuestaCiudadana servicio;

    @BeforeEach
    void preparar() {
        reportes = mock(ReporteRepository.class);
        historial = mock(HistorialReporteRepository.class);
        consulta = mock(ServicioConsultaCiudadana.class);
        perfiles = mock(ServicioPerfilCiudadano.class);
        servicio = new ServicioRespuestaCiudadana(reportes, historial, consulta, mock(ServicioOrganizaciones.class),
                perfiles);
        when(perfiles.condicionParaReportar(CIUDADANO)).thenReturn(condicion(false));
        when(consulta.plazoReapertura(any())).thenReturn(15);
    }

    @Test
    void reabreElReporteYGuardaElMotivoEnSuHistorial() {
        Reporte r = dado(resuelto(CIUDADANO));

        servicio.reabrir(CIUDADANO, FOLIO, "  El bache volvió a abrirse con la lluvia.  ");

        assertEquals(EstadoReporte.REABIERTO, r.getEstado());
        ArgumentCaptor<HistorialReporte> evento = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(historial).save(evento.capture());
        assertEquals("reapertura", evento.getValue().getAccion());
        assertEquals("Indicaste que el problema persiste: El bache volvió a abrirse con la lluvia.",
                evento.getValue().getObservaciones());
        verify(consulta).detalle(CIUDADANO, FOLIO);
    }

    @Test
    void unReporteDeOtraCuentaRespondeNoEncontrado() {
        dado(resuelto(99L));

        assertEquals(404, assertThrows(ReglaNegocioException.class,
                () -> servicio.reabrir(CIUDADANO, FOLIO, "El problema sigue igual.")).getEstadoHttp());
    }

    @Test
    void unaCuentaSuspendidaNoPuedeResponder() {
        dado(resuelto(CIUDADANO));
        when(perfiles.condicionParaReportar(CIUDADANO)).thenReturn(condicion(true));

        assertEquals(403, assertThrows(ReglaNegocioException.class,
                () -> servicio.reabrir(CIUDADANO, FOLIO, "El problema sigue igual.")).getEstadoHttp());
        assertEquals(403, assertThrows(ReglaNegocioException.class,
                () -> servicio.confirmar(CIUDADANO, FOLIO)).getEstadoHttp());
    }

    @Test
    void despuesDeConfirmarYaNoSeReabre() {
        Reporte r = dado(resuelto(CIUDADANO));
        when(consulta.confirmado(r.getId())).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.reabrir(CIUDADANO, FOLIO, "El problema sigue igual."));

        assertEquals("YA_CONFIRMADO", error.getCodigo());
        assertEquals(EstadoReporte.RESUELTO, r.getEstado());
    }

    @Test
    void confirmarRegistraElEventoSinCambiarElEstado() {
        Reporte r = dado(resuelto(CIUDADANO));

        servicio.confirmar(CIUDADANO, FOLIO);

        assertEquals(EstadoReporte.RESUELTO, r.getEstado());
        ArgumentCaptor<HistorialReporte> evento = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(historial).save(evento.capture());
        assertEquals(HistorialReporte.ACCION_CONFIRMACION, evento.getValue().getAccion());
        assertEquals("El ciudadano confirmó que el problema se resolvió.", evento.getValue().getObservaciones());
    }

    @Test
    void soloSeConfirmaUnReporteResuelto() {
        Reporte r = resuelto(CIUDADANO);
        r.reabrir(15, Instant.now());
        dado(r);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.confirmar(CIUDADANO, FOLIO));

        assertEquals("TRANSICION_INVALIDA", error.getCodigo());
    }

    @Test
    void noSeConfirmaDosVeces() {
        Reporte r = dado(resuelto(CIUDADANO));
        when(consulta.confirmado(anyLong())).thenReturn(true);

        assertEquals("YA_CONFIRMADO", assertThrows(ReglaNegocioException.class,
                () -> servicio.confirmar(CIUDADANO, FOLIO)).getCodigo());
        verify(historial, never()).save(any());
        assertEquals(EstadoReporte.RESUELTO, r.getEstado());
    }

    private Reporte dado(Reporte r) {
        when(reportes.bloquearPorFolio(FOLIO)).thenReturn(Optional.of(r));
        return r;
    }

    private static CondicionCiudadano condicion(boolean suspendida) {
        return new CondicionCiudadano(true, false, suspendida, suspendida ? "Reportes falsos" : null, null);
    }

    private static Reporte resuelto(long ciudadanoId) {
        Reporte r = new Reporte(FOLIO, ciudadanoId, "Hay un bache enorme frente a la escuela", 18.85, -97.06,
                OrigenReporte.web);
        ReflectionTestUtils.setField(r, "id", 100L);
        r.canalizar(12L, false, 1L, 4L, MotivoCanalizacion.regla, EstadoReporte.RECIBIDO);
        r.validar();
        r.iniciarAtencion();
        r.resolver();
        return r;
    }
}
