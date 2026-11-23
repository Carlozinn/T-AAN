package mx.uv.taan.configuracion.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.infraestructura.TiempoObjetivoRepository;

class ServicioSemaforoTest {

    private static final long ORG = 1L;

    private TiempoObjetivoRepository tiempos;
    private ServicioSemaforo servicio;

    @BeforeEach
    void preparar() {
        tiempos = mock(TiempoObjetivoRepository.class);
        servicio = new ServicioSemaforo(tiempos);
    }

    @Test
    void reemplazaLaConfiguracionDeLaOrganizacion() {
        List<TiempoObjetivoEstado> nuevos = List.of(
                new TiempoObjetivoEstado("recibido", 24, 72),
                new TiempoObjetivoEstado("en_atencion", 120, 240));

        servicio.configurar(ORG, nuevos);

        verify(tiempos).reemplazar(ORG, nuevos);
    }

    @Test
    void unEstadoFinalNoSeConfigura() {
        assertCodigo("ESTADO_NO_CONFIGURABLE", new TiempoObjetivoEstado("resuelto", 24, 72));
        assertCodigo("ESTADO_NO_CONFIGURABLE", new TiempoObjetivoEstado("pendiente_canalizacion", 24, 72));
        assertCodigo("ESTADO_NO_CONFIGURABLE", new TiempoObjetivoEstado(null, 24, 72));
    }

    @Test
    void unEstadoNoSeRepite() {
        assertCodigo("ESTADO_REPETIDO", new TiempoObjetivoEstado("recibido", 24, 72),
                new TiempoObjetivoEstado("recibido", 48, 96));
    }

    @Test
    void elRojoDebeSerMayorQueElAmarillo() {
        assertCodigo("VALIDACION", new TiempoObjetivoEstado("asignado", 48, 48));
        assertCodigo("VALIDACION", new TiempoObjetivoEstado("asignado", 0, 48));
        assertCodigo("VALIDACION", new TiempoObjetivoEstado("asignado", null, 48));
    }

    private void assertCodigo(String codigo, TiempoObjetivoEstado... elementos) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.configurar(ORG, List.of(elementos)));
        assertEquals(400, error.getEstadoHttp());
        assertEquals(codigo, error.getCodigo());
        verify(tiempos, never()).reemplazar(anyLong(), anyList());
    }
}
