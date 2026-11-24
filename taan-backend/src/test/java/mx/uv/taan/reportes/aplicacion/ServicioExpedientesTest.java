package mx.uv.taan.reportes.aplicacion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.infraestructura.BusquedaReportes;
import mx.uv.taan.reportes.infraestructura.EstadisticasReportes;
import mx.uv.taan.reportes.infraestructura.HistorialReporteRepository;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;

class ServicioExpedientesTest {

    @Test
    void registrarGuardaLosEventosYOfreceCadaUnoAlPublicadorDeAvisos() {
        HistorialReporteRepository historial = mock(HistorialReporteRepository.class);
        PublicadorAvisos avisos = mock(PublicadorAvisos.class);
        ServicioExpedientes servicio = new ServicioExpedientes(mock(ReporteRepository.class), historial,
                mock(BusquedaReportes.class), mock(EstadisticasReportes.class), avisos);
        HistorialReporte cambio = HistorialReporte.cambioPorPersonal(100L, EstadoReporte.RECIBIDO,
                EstadoReporte.RECHAZADO, "rechazo", "No procede.", 3L);
        HistorialReporte nota = HistorialReporte.nota(100L, "justificacion_rechazo", "Interna", 3L);

        servicio.registrar(cambio, nota);

        verify(historial).saveAll(List.of(cambio, nota));
        verify(avisos).publicar(cambio);
        verify(avisos).publicar(nota);
    }
}
