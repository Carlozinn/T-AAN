package mx.uv.taan.atencion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.administracion.aplicacion.OrganizacionResumen;
import mx.uv.taan.administracion.aplicacion.ServicioCategorias;
import mx.uv.taan.administracion.aplicacion.ServicioZonas;
import mx.uv.taan.administracion.dominio.NivelParticipacion;
import mx.uv.taan.archivos.aplicacion.ServicioArchivos;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.reportes.aplicacion.ServicioExpedientes;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.MotivoCanalizacion;
import mx.uv.taan.reportes.dominio.MotivoRechazo;
import mx.uv.taan.reportes.dominio.OrigenReporte;
import mx.uv.taan.reportes.dominio.Reporte;

class ServicioFlujoAtencionTest {

    private static final long ORG = 1L;
    private static final long OBRAS = 4L;

    private ServicioExpedientes expedientes;
    private ServicioAreas areas;
    private ServicioZonas zonas;
    private ServicioBandeja bandeja;
    private ServicioFlujoAtencion servicio;

    private final UsuarioActual coordinacion = new UsuarioActual(2L, Rol.coordinacion, ORG, null);
    private final UsuarioActual personalObras = new UsuarioActual(3L, Rol.personal_area, ORG, OBRAS);
    private final UsuarioActual personalAlumbrado = new UsuarioActual(5L, Rol.personal_area, ORG, 1L);
    private final UsuarioActual otraOrganizacion = new UsuarioActual(9L, Rol.coordinacion, 2L, null);

    @BeforeEach
    void preparar() {
        expedientes = mock(ServicioExpedientes.class);
        areas = mock(ServicioAreas.class);
        zonas = mock(ServicioZonas.class);
        bandeja = mock(ServicioBandeja.class);
        servicio = new ServicioFlujoAtencion(expedientes, bandeja, mock(ServicioCategorias.class), areas,
                mock(ServicioArchivos.class), zonas);
        when(zonas.organizacionesConCobertura(anyDouble(), anyDouble())).thenReturn(List.of(
                new OrganizacionResumen(ORG, "Ayuntamiento", NivelParticipacion.integrada),
                new OrganizacionResumen(2L, "Gobierno del Estado", NivelParticipacion.integrada),
                new OrganizacionResumen(3L, "CFE", NivelParticipacion.externa)));
    }

    @Test
    void losDestinosDeRedireccionNoIncluyenALaOrganizacionActual() {
        Reporte r = reporte("TAAN-2026-000001", OBRAS);
        when(bandeja.enAlcance(coordinacion, r.getFolio())).thenReturn(r);

        List<OrganizacionResumen> destinos = servicio.destinosRedireccion(coordinacion, r.getFolio());

        assertEquals(List.of(2L, 3L), destinos.stream().map(OrganizacionResumen::id).toList());
    }

    @Test
    void laRedireccionPasaElReporteAOtraOrganizacionConJustificacionInterna() {
        Reporte r = dado(reporte("TAAN-2026-000001", OBRAS));

        servicio.redirigir(personalObras, r.getFolio(), 2L, null, "Es una carretera estatal, no municipal.");

        assertEquals(EstadoReporte.RECIBIDO, r.getEstado());
        assertEquals(2L, r.getOrganizacionId());
        ArgumentCaptor<HistorialReporte> eventos = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(expedientes).registrar(eventos.capture(), eventos.capture());
        HistorialReporte cambio = eventos.getAllValues().get(0);
        assertEquals("redireccion", cambio.getAccion());
        assertEquals(ORG, cambio.getOrganizacionAnteriorId());
        assertEquals(2L, cambio.getOrganizacionNuevaId());
        assertTrue(cambio.isVisibleCiudadano());
        assertFalse(eventos.getAllValues().get(1).isVisibleCiudadano());
        verify(bandeja).armarDetalle(r, false);
    }

    @Test
    void redirigirAUnaOrganizacionExternaLaDejaEnCanalizacionExterna() {
        Reporte r = dado(reporte("TAAN-2026-000001", OBRAS));

        servicio.redirigir(coordinacion, r.getFolio(), 3L, null, "La luminaria es de la red de CFE.");

        assertEquals(EstadoReporte.CANALIZACION_EXTERNA, r.getEstado());
    }

    @Test
    void noSeRedirigeAUnaOrganizacionSinCobertura() {
        Reporte r = dado(reporte("TAAN-2026-000001", OBRAS));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.redirigir(coordinacion, r.getFolio(), 99L, null, "No nos corresponde atenderlo."));

        assertEquals("DESTINO_INVALIDO", error.getCodigo());
        assertEquals(ORG, r.getOrganizacionId());
    }

    @Test
    void noSeRedirigeALaMismaOrganizacion() {
        Reporte r = dado(reporte("TAAN-2026-000001", OBRAS));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.redirigir(coordinacion, r.getFolio(), ORG, null, "No nos corresponde atenderlo."));

        assertEquals("DESTINO_INVALIDO", error.getCodigo());
    }

    @Test
    void soloSeRedirigeUnReporteRecibido() {
        Reporte r = dado(reporte("TAAN-2026-000001", OBRAS));
        r.validar();

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.redirigir(personalObras, r.getFolio(), 2L, null, "No nos corresponde atenderlo."));

        assertEquals("TRANSICION_INVALIDA", error.getCodigo());
        verify(expedientes, never()).registrar(any(HistorialReporte.class), any(HistorialReporte.class));
    }

    @Test
    void fueraDelAlcanceRespondeNoEncontrado() {
        dado(reporte("TAAN-2026-000001", OBRAS));

        assertEquals(404, assertThrows(ReglaNegocioException.class,
                () -> servicio.iniciarAtencion(personalAlumbrado, "TAAN-2026-000001")).getEstadoHttp());
        assertEquals(404, assertThrows(ReglaNegocioException.class,
                () -> servicio.iniciarAtencion(otraOrganizacion, "TAAN-2026-000001")).getEstadoHttp());
    }

    @Test
    void elPersonalValidaLoDeSuAreaYQuedaAsignado() {
        Reporte r = dado(reporte("TAAN-2026-000001", OBRAS));

        servicio.validar(personalObras, "TAAN-2026-000001", null);

        assertEquals(EstadoReporte.ASIGNADO, r.getEstado());
        verify(expedientes).registrar(any(HistorialReporte.class));
    }

    @Test
    void soloLaCoordinacionValidaReportesSinArea() {
        dado(reporte("TAAN-2026-000001", null));

        // El personal de área ni siquiera lo ve (no es de su área): 404.
        assertEquals(404, assertThrows(ReglaNegocioException.class,
                () -> servicio.validar(personalObras, "TAAN-2026-000001", null)).getEstadoHttp());
    }

    @Test
    void elRechazoGuardaUnEventoPublicoYLaJustificacionAparte() {
        Reporte r = dado(reporte("TAAN-2026-000001", OBRAS));

        servicio.rechazar(personalObras, "TAAN-2026-000001", MotivoRechazo.no_servicio_publico, null,
                "Es un predio particular, no vía pública.");

        assertEquals(EstadoReporte.RECHAZADO, r.getEstado());
        ArgumentCaptor<HistorialReporte> eventos = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(expedientes).registrar(eventos.capture(), eventos.capture());
        assertEquals(true, eventos.getAllValues().get(0).isVisibleCiudadano());
        assertEquals(false, eventos.getAllValues().get(1).isVisibleCiudadano());
        assertEquals("Es un predio particular, no vía pública.", eventos.getAllValues().get(1).getObservaciones());
    }

    @Test
    void laReasignacionExigeJustificacion() {
        Reporte r = dado(reporte("TAAN-2026-000001", OBRAS));
        r.validar();

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.asignar(personalObras, "TAAN-2026-000001", 5L, " "));

        assertEquals(400, error.getEstadoHttp());
        assertEquals(OBRAS, r.getAreaId());
    }

    @Test
    void soloLaCoordinacionAsignaUnReporteValidado() {
        Reporte r = dado(reporte("TAAN-2026-000001", null));
        r.validar();
        // Un personal de área no lo ve; probamos la regla con uno cuya área coincidiera por error.
        ReflectionTestUtils.setField(r, "areaId", OBRAS);

        assertEquals(403, assertThrows(ReglaNegocioException.class,
                () -> servicio.asignar(personalObras, "TAAN-2026-000001", 5L, null)).getEstadoHttp());
    }

    @Test
    void elDuplicadoNoSeVinculaAUnReporteDeOtraOrganizacion() {
        Reporte r = dado(reporte("TAAN-2026-000001", OBRAS));
        Reporte ajeno = reporte("TAAN-2026-000002", OBRAS);
        ReflectionTestUtils.setField(ajeno, "organizacionId", 2L);
        when(expedientes.porFolio("TAAN-2026-000002")).thenReturn(Optional.of(ajeno));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.marcarDuplicado(coordinacion, "TAAN-2026-000001", "TAAN-2026-000002", "Mismo bache"));

        assertEquals("PRINCIPAL_INVALIDO", error.getCodigo());
        assertEquals(EstadoReporte.RECIBIDO, r.getEstado());
        verify(expedientes, never()).registrar(any(HistorialReporte.class), any(HistorialReporte.class));
    }

    @Test
    void elDuplicadoQuedaLigadoAlPrincipal() {
        Reporte r = dado(reporte("TAAN-2026-000001", OBRAS));
        Reporte principal = reporte("TAAN-2026-000002", OBRAS);
        ReflectionTestUtils.setField(principal, "id", 200L);
        when(expedientes.porFolio("TAAN-2026-000002")).thenReturn(Optional.of(principal));

        servicio.marcarDuplicado(personalObras, "TAAN-2026-000001", "TAAN-2026-000002", "Mismo bache, misma calle");

        assertEquals(EstadoReporte.DUPLICADO, r.getEstado());
        assertEquals(200L, r.getReportePrincipalId());
    }

    private Reporte dado(Reporte r) {
        when(expedientes.bloquearPorFolio(r.getFolio())).thenReturn(r);
        return r;
    }

    private static Reporte reporte(String folio, Long areaId) {
        Reporte r = new Reporte(folio, 7L, "Hay un bache enorme frente a la escuela", 18.85, -97.06, OrigenReporte.web);
        ReflectionTestUtils.setField(r, "id", 100L);
        r.canalizar(12L, false, ORG, areaId, MotivoCanalizacion.regla, EstadoReporte.RECIBIDO);
        return r;
    }
}
