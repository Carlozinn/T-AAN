package mx.uv.taan.reportes.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import mx.uv.taan.administracion.aplicacion.CategoriaVista;
import mx.uv.taan.administracion.aplicacion.MotorReglas;
import mx.uv.taan.administracion.aplicacion.ServicioCategorias;
import mx.uv.taan.ia.ResultadoClasificacion;
import mx.uv.taan.reportes.dominio.EstadoReporte;
import mx.uv.taan.reportes.dominio.HistorialReporte;
import mx.uv.taan.reportes.dominio.MotivoCanalizacion;
import mx.uv.taan.reportes.dominio.OrigenReporte;
import mx.uv.taan.reportes.dominio.Reporte;
import mx.uv.taan.reportes.infraestructura.HistorialReporteRepository;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;
import mx.uv.taan.reportes.infraestructura.ResultadoIaRepository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ServicioCanalizacionTest {

    private ReporteRepository reportes;
    private HistorialReporteRepository historial;
    private MotorReglas motor;
    private ServicioCanalizacion servicio;
    private Reporte reporte;

    @BeforeEach
    void preparar() {
        reportes = mock(ReporteRepository.class);
        historial = mock(HistorialReporteRepository.class);
        motor = mock(MotorReglas.class);
        ServicioCategorias categorias = mock(ServicioCategorias.class);
        when(categorias.listarActivas()).thenReturn(List.of(
                new CategoriaVista(4L, "Vialidades", null, true),
                new CategoriaVista(12L, "Bache", 4L, true),
                new CategoriaVista(1L, "Alumbrado público", null, true)));
        ObjectMapper json = JsonMapper.builder().build();
        servicio = new ServicioCanalizacion(reportes, historial, mock(ResultadoIaRepository.class), categorias, motor, json);

        reporte = new Reporte("TAAN-2026-000001", 7L, "Hay un bache enorme en la calle", 18.85, -97.06, OrigenReporte.app);
        ReflectionTestUtils.setField(reporte, "id", 100L);
        when(reportes.bloquear(100L)).thenReturn(Optional.of(reporte));
    }

    @Test
    void resuelveCategoriaYSubcategoriaSinImportarMayusculas() {
        assertEquals(12L, servicio.resolverCategoria(new ResultadoClasificacion("vialidades", "BACHE", false)));
        assertEquals(4L, servicio.resolverCategoria(new ResultadoClasificacion("Vialidades", null, false)));
        assertEquals(4L, servicio.resolverCategoria(new ResultadoClasificacion("Vialidades", "Inventada", false)));
        assertNull(servicio.resolverCategoria(new ResultadoClasificacion("Categoría que no existe", null, false)));
        assertNull(servicio.resolverCategoria(ResultadoClasificacion.sinClasificacion()));
    }

    @Test
    void canalizaARecibidoConLaOrganizacionYElAreaDelMotor() {
        when(motor.determinar(anyDouble(), anyDouble(), eq(12L))).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 1L, "Ayuntamiento de Ixtaczoquitlán", 4L, false, MotorReglas.Motivo.regla)));

        servicio.aplicar(100L, new ResultadoClasificacion("Vialidades", "Bache", false));

        assertEquals(EstadoReporte.RECIBIDO, reporte.getEstado());
        assertEquals(4L, reporte.getAreaId());
        assertEquals(MotivoCanalizacion.regla, reporte.getMotivoCanalizacion());
        assertEquals(1, reporte.getIntentosIa());
        ArgumentCaptor<HistorialReporte> evento = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(historial).save(evento.capture());
        assertEquals("canalizacion", evento.getValue().getAccion());
    }

    @Test
    void sinClasificacionVaALaReceptoraConEseMotivo() {
        when(motor.determinar(anyDouble(), anyDouble(), isNull())).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 1L, "Ayuntamiento", null, false, MotorReglas.Motivo.sin_clasificacion)));

        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion());

        assertEquals(EstadoReporte.RECIBIDO, reporte.getEstado());
        assertEquals(MotivoCanalizacion.sin_clasificacion, reporte.getMotivoCanalizacion());
        assertNull(reporte.getCategoriaSugeridaId());
    }

    @Test
    void unaOrganizacionExternaDejaElReporteEnCanalizacionExterna() {
        when(motor.determinar(anyDouble(), anyDouble(), any())).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 2L, "CFE", null, true, MotorReglas.Motivo.regla)));

        servicio.aplicar(100L, new ResultadoClasificacion("Alumbrado público", null, false));

        assertEquals(EstadoReporte.CANALIZACION_EXTERNA, reporte.getEstado());
    }

    @Test
    void unReporteYaCanalizadoNoSeVuelveAProcesar() {
        when(motor.determinar(anyDouble(), anyDouble(), any())).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 1L, "Ayuntamiento", null, false, MotorReglas.Motivo.sin_regla)));
        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion());

        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion());

        verify(historial, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void sinZonaActivaQuedaPendiente() {
        when(motor.determinar(anyDouble(), anyDouble(), any())).thenReturn(Optional.empty());

        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion());

        assertEquals(EstadoReporte.PENDIENTE_CANALIZACION, reporte.getEstado());
        verify(historial, never()).save(any());
    }
}
