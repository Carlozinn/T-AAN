package mx.uv.taan.atencion.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import mx.uv.taan.comun.error.ReglaNegocioException;

class SenalamientoTest {

    @Test
    void nacePendienteYSeResuelveUnaSolaVez() {
        Senalamiento s = new Senalamiento(100L, 7L, 3L, "Envía reportes falsos con insultos.");
        assertEquals(EstadoSenalamiento.pendiente, s.getEstado());

        s.aceptar(1L, "Reportes falsos reiterados.");

        assertEquals(EstadoSenalamiento.aceptado, s.getEstado());
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> s.desestimar(1L, "No hay indicios de abuso."));
        assertEquals("SENALAMIENTO_RESUELTO", error.getCodigo());
        assertEquals(EstadoSenalamiento.aceptado, s.getEstado());
    }
}
