package mx.uv.taan.reportes.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import mx.uv.taan.ia.ClasificadorIa;
import mx.uv.taan.ia.IaNoDisponibleException;
import mx.uv.taan.ia.ResultadoClasificacion;

/** Reintentos cuando fallan los proveedores (RF-007 punto 5, CU-01 EX-04). */
class ProcesadorReportesTest {

    private static final long REPORTE = 100L;

    private ServicioCanalizacion canalizacion;
    private ClasificadorIa clasificador;
    private ProcesadorReportes procesador;

    @BeforeEach
    void preparar() {
        canalizacion = mock(ServicioCanalizacion.class);
        clasificador = mock(ClasificadorIa.class);
        procesador = new ProcesadorReportes(canalizacion, clasificador, 3);
        when(canalizacion.descripcionSiPendiente(REPORTE)).thenReturn(Optional.of("Hay un bache enorme"));
        when(canalizacion.catalogoParaIa()).thenReturn(List.of("Vialidades", "Vialidades / Bache"));
    }

    @Test
    void conClasificacionCanalizaDeInmediato() {
        ResultadoClasificacion bache = new ResultadoClasificacion("Vialidades", "Bache", false, null, "gemini",
                "gemini-flash-lite-latest", null);
        when(clasificador.clasificar(anyString(), anyList())).thenReturn(bache);

        procesador.procesar(REPORTE);

        verify(canalizacion).aplicar(REPORTE, bache);
        verify(canalizacion, never()).registrarFalloIa(anyLong(), anyInt());
    }

    @Test
    void siLaIaFallaYQuedanIntentosElReporteSigueSinCanalizar() {
        when(clasificador.clasificar(anyString(), anyList())).thenThrow(new IaNoDisponibleException("caída"));
        when(canalizacion.registrarFalloIa(REPORTE, 3)).thenReturn(false);

        procesador.procesar(REPORTE);

        verify(canalizacion, never()).aplicar(anyLong(), any());
    }

    @Test
    void alAgotarLosIntentosSeCanalizaSinClasificacion() {
        when(clasificador.clasificar(anyString(), anyList())).thenThrow(new IaNoDisponibleException("caída"));
        when(canalizacion.registrarFalloIa(REPORTE, 3)).thenReturn(true);

        procesador.procesar(REPORTE);

        ArgumentCaptor<ResultadoClasificacion> resultado = ArgumentCaptor.forClass(ResultadoClasificacion.class);
        verify(canalizacion).aplicar(eq(REPORTE), resultado.capture());
        assertEquals(ResultadoClasificacion.IA_NO_DISPONIBLE, resultado.getValue().razon());
    }

    @Test
    void unReporteQueYaNoEstaPendienteNoLlamaALaIa() {
        when(canalizacion.descripcionSiPendiente(REPORTE)).thenReturn(Optional.empty());

        procesador.procesar(REPORTE);

        verify(clasificador, never()).clasificar(anyString(), anyList());
    }
}
