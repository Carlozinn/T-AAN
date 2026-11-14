package mx.uv.taan.reportes.dominio;

import static mx.uv.taan.reportes.dominio.EstadoReporte.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Comprueba que la máquina de estados es exactamente la del Anexo G. */
class EstadoReporteTest {

    @Test
    void permiteLasCatorceTransicionesDelAnexoG() {
        EstadoReporte[][] validas = {
            {PENDIENTE_CANALIZACION, RECIBIDO}, {PENDIENTE_CANALIZACION, CANALIZACION_EXTERNA},
            {RECIBIDO, ASIGNADO}, {RECIBIDO, VALIDADO}, {RECIBIDO, RECHAZADO}, {RECIBIDO, DUPLICADO},
            {RECIBIDO, RECIBIDO}, {RECIBIDO, CANALIZACION_EXTERNA},
            {VALIDADO, ASIGNADO}, {ASIGNADO, ASIGNADO}, {ASIGNADO, EN_ATENCION},
            {EN_ATENCION, RESUELTO}, {RESUELTO, REABIERTO}, {REABIERTO, EN_ATENCION},
        };
        int permitidas = 0;
        for (EstadoReporte origen : values()) {
            for (EstadoReporte destino : values()) {
                if (origen.puedePasarA(destino)) {
                    permitidas++;
                }
            }
        }
        assertEquals(validas.length, permitidas);
        for (EstadoReporte[] t : validas) {
            assertTrue(t[0].puedePasarA(t[1]), t[0] + " -> " + t[1]);
        }
    }

    @Test
    void losEstadosFinalesNoTienenSalidas() {
        assertTrue(RECHAZADO.esFinal());
        assertTrue(DUPLICADO.esFinal());
        assertTrue(CANALIZACION_EXTERNA.esFinal());
        assertFalse(RESUELTO.esFinal()); // puede reabrirse una vez
    }

    @Test
    void unaTransicionInexistenteResponde409() {
        TransicionInvalidaException e = assertThrows(TransicionInvalidaException.class,
                () -> RECHAZADO.validarTransicion(EN_ATENCION));
        assertEquals(409, e.getEstadoHttp());
        assertEquals("TRANSICION_INVALIDA", e.getCodigo());
    }

    @Test
    void elCiudadanoVePendienteComoEnRevision() {
        assertEquals("En revisión", PENDIENTE_CANALIZACION.textoCiudadano());
        assertEquals(EN_ATENCION, EstadoReporte.desdeValorBd("en_atencion"));
    }
}
