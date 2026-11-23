package mx.uv.taan.reportes.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    private static Reporte recibido(Long areaId) {
        Reporte r = new Reporte("TAAN-2026-000001", 7L, "Hay un bache enorme frente a la escuela", 18.85, -97.06,
                OrigenReporte.web);
        ReflectionTestUtils.setField(r, "id", 100L);
        r.canalizar(12L, false, 1L, areaId, MotivoCanalizacion.regla, EstadoReporte.RECIBIDO);
        return r;
    }
}
