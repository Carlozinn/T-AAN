package mx.uv.taan.atencion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.atencion.dominio.EstadoSenalamiento;
import mx.uv.taan.atencion.dominio.Senalamiento;
import mx.uv.taan.atencion.infraestructura.SenalamientoRepository;
import mx.uv.taan.comun.api.Pagina;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.aplicacion.ServicioCuentasInstitucionales;
import mx.uv.taan.cuentas.aplicacion.ServicioSuspensiones;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.reportes.aplicacion.ServicioExpedientes;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.MotivoCanalizacion;
import mx.uv.taan.reportes.dominio.OrigenReporte;
import mx.uv.taan.reportes.dominio.Reporte;

class ServicioSenalamientosTest {

    private static final long ORG = 1L;
    private static final long OBRAS = 4L;
    private static final long CIUDADANO = 7L;

    private SenalamientoRepository senalamientos;
    private ServicioExpedientes expedientes;
    private ServicioSuspensiones suspensiones;
    private ServicioSenalamientos servicio;

    private final UsuarioActual personalObras = new UsuarioActual(3L, Rol.personal_area, ORG, OBRAS);
    private final UsuarioActual personalAlumbrado = new UsuarioActual(5L, Rol.personal_area, ORG, 1L);
    private final UsuarioActual administrador = new UsuarioActual(1L, Rol.administrador, null, null);

    @BeforeEach
    void preparar() {
        senalamientos = mock(SenalamientoRepository.class);
        expedientes = mock(ServicioExpedientes.class);
        suspensiones = mock(ServicioSuspensiones.class);
        ServicioCuentasInstitucionales cuentas = mock(ServicioCuentasInstitucionales.class);
        ServicioOrganizaciones organizaciones = mock(ServicioOrganizaciones.class);
        servicio = new ServicioSenalamientos(senalamientos, expedientes, suspensiones, cuentas, organizaciones);
        when(cuentas.nombres(anyCollection())).thenReturn(Map.of(3L, "Personal de Obras"));
        when(organizaciones.nombres(anyCollection())).thenReturn(Map.of(ORG, "Ayuntamiento"));
        when(suspensiones.nombres(anyCollection())).thenReturn(Map.of(CIUDADANO, "Vecino"));
        when(suspensiones.suspender(anyLong(), anyString(), any())).thenReturn(true);
    }

    // ------------------------------------------------------------------ señalar

    @Test
    void senalarRegistraElSenalamientoPendienteSinCambiarElEstado() {
        Reporte r = dado(reporte(CIUDADANO));

        servicio.senalar(personalObras, r.getFolio(), "  Envía reportes falsos con insultos.  ");

        ArgumentCaptor<Senalamiento> guardado = ArgumentCaptor.forClass(Senalamiento.class);
        verify(senalamientos).save(guardado.capture());
        assertEquals(EstadoSenalamiento.pendiente, guardado.getValue().getEstado());
        assertEquals(CIUDADANO, guardado.getValue().getCiudadanoId());
        assertEquals(3L, guardado.getValue().getSenaladoPor());
        assertEquals("Envía reportes falsos con insultos.", guardado.getValue().getMotivo());
        assertEquals(EstadoReporte.RECIBIDO, r.getEstado());
    }

    @Test
    void unReporteFueraDelAlcanceResponde404() {
        Reporte r = dado(reporte(CIUDADANO));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.senalar(personalAlumbrado, r.getFolio(), "Envía reportes falsos con insultos."));

        assertEquals(404, error.getEstadoHttp());
        verify(senalamientos, never()).save(any());
    }

    @Test
    void unReporteConSenalamientoPendienteNoSeVuelveASenalar() {
        Reporte r = dado(reporte(CIUDADANO));
        when(senalamientos.existsByReporteIdAndEstado(r.getId(), EstadoSenalamiento.pendiente)).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.senalar(personalObras, r.getFolio(), "Envía reportes falsos con insultos."));

        assertEquals("SENALAMIENTO_PENDIENTE", error.getCodigo());
        assertEquals(409, error.getEstadoHttp());
    }

    @Test
    void unReporteDeUnaCuentaEliminadaNoSePuedeSenalar() {
        Reporte r = dado(reporte(null));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.senalar(personalObras, r.getFolio(), "Envía reportes falsos con insultos."));

        assertEquals("CUENTA_ELIMINADA", error.getCodigo());
    }

    // ------------------------------------------------------------------ resolver

    @Test
    void desestimarNoSuspendeLaCuenta() {
        Senalamiento s = pendiente();

        SenalamientoVista vista = servicio.resolver(administrador, 50L, false, "No hay indicios de abuso.", null);

        assertEquals(EstadoSenalamiento.desestimado, vista.estado());
        assertEquals("No hay indicios de abuso.", s.getResolucion());
        verify(suspensiones, never()).suspender(anyLong(), anyString(), any());
    }

    @Test
    void laPrimeraSuspensionTieneLaVigenciaIndicada() {
        pendiente();

        SenalamientoVista vista = servicio.resolver(administrador, 50L, true, "Reportes falsos reiterados.", 7);

        ArgumentCaptor<Instant> hasta = ArgumentCaptor.forClass(Instant.class);
        verify(suspensiones).suspender(eq(CIUDADANO), eq("Reportes falsos reiterados."), hasta.capture());
        Duration restante = Duration.between(Instant.now(), hasta.getValue());
        assertTrue(restante.compareTo(Duration.ofDays(7).minusMinutes(1)) > 0 && restante.compareTo(Duration.ofDays(7)) <= 0);
        assertEquals(EstadoSenalamiento.aceptado, vista.estado());
    }

    @Test
    void laPrimeraSuspensionNoPuedeSerIndefinida() {
        Senalamiento s = pendiente();

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.resolver(administrador, 50L, true, "Reportes falsos reiterados.", null));

        assertEquals(400, error.getEstadoHttp());
        verify(suspensiones, never()).suspender(anyLong(), anyString(), any());
        assertEquals(EstadoSenalamiento.aceptado, s.getEstado(), "la transacción revierte este cambio");
    }

    @Test
    void laReincidenciaAdmiteUnaSuspensionIndefinida() {
        pendiente();
        when(senalamientos.countByCiudadanoIdAndEstado(CIUDADANO, EstadoSenalamiento.aceptado)).thenReturn(1L);

        servicio.resolver(administrador, 50L, true, "Reincide con reportes falsos.", null);

        verify(suspensiones).suspender(eq(CIUDADANO), eq("Reincide con reportes falsos."), isNull());
    }

    @Test
    void unSenalamientoYaResueltoResponde409SinTocarLaCuenta() {
        Senalamiento s = pendiente();
        s.desestimar(1L, "No hay indicios de abuso.");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.resolver(administrador, 50L, true, "Reportes falsos reiterados.", 7));

        assertEquals("SENALAMIENTO_RESUELTO", error.getCodigo());
        verify(suspensiones, never()).suspender(anyLong(), anyString(), any());
    }

    @Test
    void noSePuedeSuspenderUnaCuentaEliminada() {
        Senalamiento s = pendiente();
        ReflectionTestUtils.setField(s, "ciudadanoId", null);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.resolver(administrador, 50L, true, "Reportes falsos reiterados.", 7));

        assertEquals("CUENTA_ELIMINADA", error.getCodigo());
    }

    @Test
    void unSenalamientoInexistenteResponde404() {
        when(senalamientos.bloquear(99L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.resolver(administrador, 99L, false, "No hay indicios de abuso.", null));

        assertEquals(404, error.getEstadoHttp());
    }

    // ------------------------------------------------------------------ listar

    @Test
    void elListadoMuestraElReporteYLosAntecedentesSinContarElPropio() {
        Senalamiento s = senalamiento();
        when(senalamientos.findByEstadoOrderByCreadoEnAscIdAsc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(s), PageRequest.of(0, 20), 1));
        when(expedientes.porIds(List.of(100L))).thenReturn(Map.of(100L, reporte(CIUDADANO)));
        // La cuenta tiene 3 señalamientos (este incluido) y una suspensión anterior.
        when(senalamientos.antecedentes(anyCollection(), eq(EstadoSenalamiento.aceptado))).thenReturn(List.<Object[]>of(new Object[] {CIUDADANO, 3L, 1L}));

        Pagina<SenalamientoVista> pagina = servicio.listar(EstadoSenalamiento.pendiente, 0, 20);

        SenalamientoVista vista = pagina.contenido().get(0);
        assertEquals("TAAN-2026-000001", vista.folio());
        assertEquals("Hay un bache enorme frente a la escuela", vista.descripcion());
        assertEquals("Vecino", vista.ciudadano().nombre());
        assertEquals("Ayuntamiento", vista.organizacion());
        assertEquals("Personal de Obras", vista.senaladoPor());
        assertEquals(2, vista.senalamientosPrevios());
        assertEquals(1, vista.suspensionesPrevias());
    }

    @Test
    void unaCuentaEliminadaApareceSinCiudadano() {
        Senalamiento s = senalamiento();
        ReflectionTestUtils.setField(s, "ciudadanoId", null);
        when(senalamientos.findByEstadoOrderByCreadoEnAscIdAsc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(s), PageRequest.of(0, 20), 1));
        when(expedientes.porIds(List.of(100L))).thenReturn(Map.of(100L, reporte(null)));

        SenalamientoVista vista = servicio.listar(EstadoSenalamiento.pendiente, 0, 20).contenido().get(0);

        assertNull(vista.ciudadano());
        assertEquals(0, vista.senalamientosPrevios());
    }

    // ------------------------------------------------------------------ apoyo

    private Senalamiento pendiente() {
        Senalamiento s = senalamiento();
        when(senalamientos.bloquear(50L)).thenReturn(Optional.of(s));
        when(expedientes.porIds(List.of(100L))).thenReturn(Map.of(100L, reporte(CIUDADANO)));
        return s;
    }

    private static Senalamiento senalamiento() {
        Senalamiento s = new Senalamiento(100L, CIUDADANO, 3L, "Envía reportes falsos con insultos.");
        ReflectionTestUtils.setField(s, "id", 50L);
        return s;
    }

    private Reporte dado(Reporte r) {
        when(expedientes.bloquearPorFolio(r.getFolio())).thenReturn(r);
        return r;
    }

    /** ciudadanoId = null: la cuenta se eliminó y el reporte quedó desvinculado. */
    private static Reporte reporte(Long ciudadanoId) {
        Reporte r = new Reporte("TAAN-2026-000001", CIUDADANO, "Hay un bache enorme frente a la escuela", 18.85,
                -97.06, OrigenReporte.web);
        ReflectionTestUtils.setField(r, "id", 100L);
        ReflectionTestUtils.setField(r, "ciudadanoId", ciudadanoId);
        r.canalizar(12L, false, ORG, OBRAS, MotivoCanalizacion.regla, EstadoReporte.RECIBIDO);
        return r;
    }
}
