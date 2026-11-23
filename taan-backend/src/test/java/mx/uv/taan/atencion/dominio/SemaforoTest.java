package mx.uv.taan.atencion.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import mx.uv.taan.configuracion.aplicacion.TiempoObjetivo;

class SemaforoTest {

    private static final TiempoObjetivo RECIBIDO = new TiempoObjetivo(24, 72);
    private static final Instant AHORA = Instant.parse("2026-11-21T12:00:00Z");

    @Test
    void calculaElColorSegunLasHorasEnElEstado() {
        assertEquals(Semaforo.verde, Semaforo.calcular(RECIBIDO, AHORA.minusSeconds(23 * 3600), AHORA));
        assertEquals(Semaforo.amarillo, Semaforo.calcular(RECIBIDO, AHORA.minusSeconds(24 * 3600), AHORA));
        assertEquals(Semaforo.amarillo, Semaforo.calcular(RECIBIDO, AHORA.minusSeconds(71 * 3600), AHORA));
        assertEquals(Semaforo.rojo, Semaforo.calcular(RECIBIDO, AHORA.minusSeconds(72 * 3600), AHORA));
    }

    @Test
    void sinTiempoConfiguradoNoHayColor() {
        assertEquals(Semaforo.sin_tiempo, Semaforo.calcular(null, AHORA.minusSeconds(999 * 3600), AHORA));
    }
}
