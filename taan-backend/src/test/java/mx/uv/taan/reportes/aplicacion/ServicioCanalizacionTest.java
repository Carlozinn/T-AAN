package mx.uv.taan.reportes.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
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
    private ResultadoIaRepository resultadosIa;
    private ServicioCanalizacion servicio;
    private Reporte reporte;
    private final ObjectMapper json = JsonMapper.builder().build();

    @BeforeEach
    void preparar() {
        reportes = mock(ReporteRepository.class);
        historial = mock(HistorialReporteRepository.class);
        motor = mock(MotorReglas.class);
        resultadosIa = mock(ResultadoIaRepository.class);
        ServicioCategorias categorias = mock(ServicioCategorias.class);
        when(categorias.listarActivas()).thenReturn(List.of(
                new CategoriaVista(4L, "Vialidades", null, true),
                new CategoriaVista(12L, "Bache", 4L, true),
                new CategoriaVista(1L, "Alumbrado público", null, true)));
        servicio = new ServicioCanalizacion(reportes, historial, resultadosIa, categorias, motor, json);

        reporte = new Reporte("TAAN-2026-000001", 7L, "Hay un bache enorme en la calle", 18.85, -97.06, OrigenReporte.app);
        ReflectionTestUtils.setField(reporte, "id", 100L);
        when(reportes.bloquear(100L)).thenReturn(Optional.of(reporte));
    }

    @Test
    void resuelveCategoriaYSubcategoriaSinImportarMayusculas() {
        assertEquals(12L, servicio.resolverCategoria(ia("vialidades", "BACHE")));
        assertEquals(4L, servicio.resolverCategoria(ia("Vialidades", null)));
        assertEquals(4L, servicio.resolverCategoria(ia("Vialidades", "Inventada")));
        assertNull(servicio.resolverCategoria(ia("Categoría que no existe", null)));
        assertNull(servicio.resolverCategoria(ResultadoClasificacion.sinClasificacion(ResultadoClasificacion.IA_NO_DISPONIBLE)));
    }

    @Test
    void canalizaARecibidoConLaOrganizacionYElAreaDelMotor() {
        when(motor.determinar(anyDouble(), anyDouble(), eq(12L))).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 1L, "Ayuntamiento de Ixtaczoquitlán", 4L, false, MotorReglas.Motivo.regla)));

        servicio.aplicar(100L, ia("Vialidades", "Bache"));

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

        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion(ResultadoClasificacion.IA_NO_DISPONIBLE));

        assertEquals(EstadoReporte.RECIBIDO, reporte.getEstado());
        assertEquals(MotivoCanalizacion.sin_clasificacion, reporte.getMotivoCanalizacion());
        assertNull(reporte.getCategoriaSugeridaId());
    }

    @Test
    void unaOrganizacionExternaDejaElReporteEnCanalizacionExterna() {
        when(motor.determinar(anyDouble(), anyDouble(), any())).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 2L, "CFE", null, true, MotorReglas.Motivo.regla)));

        servicio.aplicar(100L, ia("Alumbrado público", null));

        assertEquals(EstadoReporte.CANALIZACION_EXTERNA, reporte.getEstado());
    }

    @Test
    void unReporteYaCanalizadoNoSeVuelveAProcesar() {
        when(motor.determinar(anyDouble(), anyDouble(), any())).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 1L, "Ayuntamiento", null, false, MotorReglas.Motivo.sin_regla)));
        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion(ResultadoClasificacion.IA_NO_DISPONIBLE));

        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion(ResultadoClasificacion.IA_NO_DISPONIBLE));

        verify(historial, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void cadaFalloDeIaCuentaUnIntentoHastaElMaximo() {
        assertFalse(servicio.registrarFalloIa(100L, 3));
        assertFalse(servicio.registrarFalloIa(100L, 3));
        assertTrue(servicio.registrarFalloIa(100L, 3));

        assertEquals(3, reporte.getIntentosIa());
        assertEquals(EstadoReporte.PENDIENTE_CANALIZACION, reporte.getEstado());
    }

    @Test
    void unFalloDeIaSobreUnReporteYaCanalizadoNoCuenta() {
        when(motor.determinar(anyDouble(), anyDouble(), any())).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 1L, "Ayuntamiento", null, false, MotorReglas.Motivo.sin_clasificacion)));
        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion(ResultadoClasificacion.IA_NO_DISPONIBLE));

        assertFalse(servicio.registrarFalloIa(100L, 1));
        assertEquals(0, reporte.getIntentosIa());
    }

    @Test
    void canalizarSinRespuestaDeIaNoSumaOtroIntento() {
        when(motor.determinar(anyDouble(), anyDouble(), any())).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 1L, "Ayuntamiento", null, false, MotorReglas.Motivo.sin_clasificacion)));
        servicio.registrarFalloIa(100L, 3);

        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion(ResultadoClasificacion.IA_NO_DISPONIBLE));

        assertEquals(1, reporte.getIntentosIa());
    }

    @Test
    void resultadoIaGuardaProveedorCategoriaYIntentosSinDatosDelReporte() {
        when(motor.determinar(anyDouble(), anyDouble(), eq(12L))).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 1L, "Ayuntamiento", 4L, false, MotorReglas.Motivo.regla)));

        servicio.aplicar(100L, ia("Vialidades", "Bache"));

        ArgumentCaptor<String> guardado = ArgumentCaptor.forClass(String.class);
        verify(resultadosIa).guardar(eq(100L), guardado.capture());
        Map<?, ?> datos = json.readValue(guardado.getValue(), Map.class);
        assertEquals("gemini", datos.get("proveedor"));
        assertEquals("gemini-flash-lite-latest", datos.get("modelo"));
        assertEquals("Bache", datos.get("subcategoria"));
        assertEquals(12, datos.get("categoriaId"));
        assertEquals(1, datos.get("intentos"));
        assertFalse(datos.containsKey("razon"));
        assertFalse(guardado.getValue().contains("Hay un bache enorme"));
    }

    @Test
    void sinCategoriaResultadoIaExplicaLaRazon() {
        when(motor.determinar(anyDouble(), anyDouble(), isNull())).thenReturn(Optional.of(
                new MotorReglas.Destino(1L, 1L, "Ayuntamiento", null, false, MotorReglas.Motivo.sin_clasificacion)));

        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion(ResultadoClasificacion.IA_NO_DISPONIBLE));

        ArgumentCaptor<String> guardado = ArgumentCaptor.forClass(String.class);
        verify(resultadosIa).guardar(eq(100L), guardado.capture());
        Map<?, ?> datos = json.readValue(guardado.getValue(), Map.class);
        assertEquals(ResultadoClasificacion.IA_NO_DISPONIBLE, datos.get("razon"));
        assertNull(datos.get("proveedor"));
    }

    @Test
    void sinZonaActivaQuedaPendiente() {
        when(motor.determinar(anyDouble(), anyDouble(), any())).thenReturn(Optional.empty());

        servicio.aplicar(100L, ResultadoClasificacion.sinClasificacion(ResultadoClasificacion.IA_NO_DISPONIBLE));

        assertEquals(EstadoReporte.PENDIENTE_CANALIZACION, reporte.getEstado());
        verify(historial, never()).save(any());
    }

    /** Respuesta de Gemini ya validada por el adaptador. */
    private static ResultadoClasificacion ia(String categoria, String subcategoria) {
        return new ResultadoClasificacion(categoria, subcategoria, false, null, "gemini", "gemini-flash-lite-latest", null);
    }
}
