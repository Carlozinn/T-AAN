package mx.uv.taan.reportes.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.comun.error.ReglaNegocioException;

/** Transiciones del personal sobre la entidad: la entidad no permite saltarse el diagrama (Anexo G). */
class ReporteTest {

    @Test
    void conAreaDeReglaValidarLoDejaAsignado() {
        Reporte r = recibido(4L);

        assertEquals(EstadoReporte.ASIGNADO, r.validar());
    }

    @Test
    void sinAreaValidarLoDejaValidado() {
        Reporte r = recibido(null);

        assertEquals(EstadoReporte.VALIDADO, r.validar());
    }

    @Test
    void noSeValidaDosVeces() {
        Reporte r = recibido(4L);
        r.validar();

        assertThrows(TransicionInvalidaException.class, r::validar);
    }

    @Test
    void unReporteRecibidoNoSeAsignaSinValidar() {
        Reporte r = recibido(null);

        assertThrows(TransicionInvalidaException.class, () -> r.asignar(4L));
    }

    @Test
    void laReasignacionNoReiniciaElSemaforo() {
        Reporte r = recibido(4L);
        r.validar();
        Instant desdeAsignado = Instant.parse("2026-11-01T10:00:00Z");
        ReflectionTestUtils.setField(r, "fechaUltimoEstado", desdeAsignado);

        r.asignar(5L);

        assertEquals(5L, r.getAreaId());
        assertEquals(desdeAsignado, r.getFechaUltimoEstado());
    }

    @Test
    void elFlujoCompletoTerminaResueltoConFecha() {
        Reporte r = recibido(4L);
        r.validar();
        r.iniciarAtencion();
        r.resolver();

        assertEquals(EstadoReporte.RESUELTO, r.getEstado());
        assertNotNull(r.getFechaResolucion());
    }

    @Test
    void noSeResuelveSinPasarPorEnAtencion() {
        Reporte r = recibido(4L);
        r.validar();

        assertThrows(TransicionInvalidaException.class, r::resolver);
    }

    @Test
    void elRechazoGuardaElMotivoPublico() {
        Reporte r = recibido(4L);
        r.rechazar(MotivoRechazo.ya_atendido, "Se reparó la semana pasada.");

        assertEquals(EstadoReporte.RECHAZADO, r.getEstado());
        assertEquals(MotivoRechazo.ya_atendido, r.getMotivoRechazo());
        assertThrows(TransicionInvalidaException.class, r::iniciarAtencion);
    }

    @Test
    void unReporteNoEsDuplicadoDeSiMismo() {
        Reporte r = recibido(4L);

        assertThrows(IllegalArgumentException.class, () -> r.marcarDuplicado(100L));
    }

    @Test
    void laRedireccionCambiaDeOrganizacionYReiniciaElSemaforo() {
        Reporte r = recibido(4L);
        Instant antes = Instant.parse("2026-11-01T10:00:00Z");
        ReflectionTestUtils.setField(r, "fechaUltimoEstado", antes);

        r.redirigir(2L, false);

        assertEquals(EstadoReporte.RECIBIDO, r.getEstado());
        assertEquals(2L, r.getOrganizacionId());
        assertNull(r.getAreaId());
        assertEquals(MotivoCanalizacion.redireccion, r.getMotivoCanalizacion());
        assertTrue(r.getFechaUltimoEstado().isAfter(antes));
    }

    @Test
    void redirigirAUnaExternaLaDejaEnCanalizacionExterna() {
        Reporte r = recibido(null);

        r.redirigir(3L, true);

        assertEquals(EstadoReporte.CANALIZACION_EXTERNA, r.getEstado());
    }

    @Test
    void soloSeRedirigeUnReporteRecibido() {
        Reporte r = recibido(4L);
        r.validar();

        assertThrows(TransicionInvalidaException.class, () -> r.redirigir(2L, false));
    }

    @Test
    void unReporteResueltoSeReabreUnaVezEnLaMismaArea() {
        Reporte r = resuelto();

        r.reabrir(15, Instant.now());

        assertEquals(EstadoReporte.REABIERTO, r.getEstado());
        assertTrue(r.isReabierto());
        assertEquals(4L, r.getAreaId());
    }

    @Test
    void noSeReabreDosVeces() {
        Reporte r = resuelto();
        r.reabrir(15, Instant.now());
        r.iniciarAtencion();
        r.resolver();

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> r.reabrir(15, Instant.now()));

        assertEquals("YA_REABIERTO", error.getCodigo());
    }

    @Test
    void noSeReabreFueraDelPlazo() {
        Reporte r = resuelto();
        Instant despues = r.getFechaResolucion().plus(Duration.ofDays(15));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> r.reabrir(15, despues));

        assertEquals("PLAZO_VENCIDO", error.getCodigo());
        assertEquals(EstadoReporte.RESUELTO, r.getEstado());
    }

    @Test
    void soloSeReabreUnReporteResuelto() {
        Reporte r = recibido(4L);

        assertThrows(TransicionInvalidaException.class, () -> r.reabrir(15, Instant.now()));
    }

    private static Reporte resuelto() {
        Reporte r = recibido(4L);
        r.validar();
        r.iniciarAtencion();
        r.resolver();
        return r;
    }

    private static Reporte recibido(Long areaId) {
        Reporte r = new Reporte("TAAN-2026-000001", 7L, "Hay un bache enorme frente a la escuela", 18.85, -97.06,
                OrigenReporte.web);
        ReflectionTestUtils.setField(r, "id", 100L);
        r.canalizar(12L, false, 1L, areaId, MotivoCanalizacion.regla, EstadoReporte.RECIBIDO);
        return r;
    }
}
