package mx.uv.taan.reportes.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import mx.uv.taan.administracion.aplicacion.CoberturaVista;
import mx.uv.taan.administracion.aplicacion.ServicioZonas;
import mx.uv.taan.archivos.aplicacion.ServicioArchivos;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.aplicacion.CondicionCiudadano;
import mx.uv.taan.cuentas.aplicacion.ServicioPerfilCiudadano;
import mx.uv.taan.reportes.dominio.OrigenReporte;
import mx.uv.taan.reportes.dominio.Reporte;
import mx.uv.taan.reportes.infraestructura.HistorialReporteRepository;
import mx.uv.taan.reportes.infraestructura.RegistroEnvios;
import mx.uv.taan.reportes.infraestructura.ReporteRepository;
import mx.uv.taan.reportes.infraestructura.SecuenciaFolios;

class ServicioRegistroReportesTest {

    private static final long CIUDADANO = 7L;
    private static final String IP = "127.0.0.1";

    private ReporteRepository reportes;
    private RegistroEnvios envios;
    private ServicioPerfilCiudadano perfiles;
    private ServicioZonas zonas;
    private ServicioArchivos archivos;
    private ApplicationEventPublisher eventos;
    private ServicioRegistroReportes servicio;

    @BeforeEach
    void preparar() {
        reportes = mock(ReporteRepository.class);
        envios = mock(RegistroEnvios.class);
        perfiles = mock(ServicioPerfilCiudadano.class);
        zonas = mock(ServicioZonas.class);
        archivos = mock(ServicioArchivos.class);
        eventos = mock(ApplicationEventPublisher.class);
        SecuenciaFolios folios = mock(SecuenciaFolios.class);
        when(folios.siguiente()).thenReturn("TAAN-2026-000001");
        when(reportes.save(any())).thenAnswer(i -> {
            Reporte r = i.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 100L);
            return r;
        });
        when(perfiles.condicionParaReportar(CIUDADANO)).thenReturn(cuentaEnRegla());
        when(zonas.cobertura(anyDouble(), anyDouble())).thenReturn(new CoberturaVista(true, "Cabecera"));
        servicio = new ServicioRegistroReportes(reportes, mock(HistorialReporteRepository.class), folios, envios,
                perfiles, zonas, archivos, eventos);
    }

    @Test
    void registraElReporteEnRevisionYArrancaElProcesamiento() {
        ReporteCreado creado = servicio.registrar(CIUDADANO, nuevo(null), List.of(), IP, "JUnit");

        assertEquals("TAAN-2026-000001", creado.folio());
        assertEquals("pendiente_canalizacion", creado.estado());
        assertEquals("En revisión", creado.estadoTexto());
        verify(envios).registrarAceptado(IP, CIUDADANO, "JUnit", 100L);
        verify(eventos).publishEvent(new ReporteRegistrado(100L));
    }

    @Test
    void elHoneypotSeRegistraComoAbusoYNoGuardaNada() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(CIUDADANO, nuevo("soy-un-bot"), List.of(), IP, "bot"));

        verify(envios).registrarRechazo(IP, CIUDADANO, "bot", "rechazado_honeypot");
        verify(reportes, never()).save(any());
    }

    @Test
    void unaCuentaSuspendidaNoReportaYSabeHastaCuando() {
        when(perfiles.condicionParaReportar(CIUDADANO)).thenReturn(
                new CondicionCiudadano(true, false, true, "Reportes falsos", Instant.parse("2026-12-01T18:00:00Z")));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(CIUDADANO, nuevo(null), List.of(), IP, "JUnit"));

        assertEquals("CUENTA_SUSPENDIDA", error.getCodigo());
        assertEquals(403, error.getEstadoHttp());
        assertTrue(error.getMessage().contains("01/12/2026"));
        verify(envios).registrarRechazo(IP, CIUDADANO, "JUnit", "rechazado_suspension");
    }

    @Test
    void sinTelefonoNoSePuedeReportar() {
        when(perfiles.condicionParaReportar(CIUDADANO)).thenReturn(new CondicionCiudadano(true, true, false, null, null));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(CIUDADANO, nuevo(null), List.of(), IP, "JUnit"));

        assertEquals("TELEFONO_REQUERIDO", error.getCodigo());
        verify(reportes, never()).save(any());
    }

    @Test
    void fueraDeCoberturaNoSeRegistra() {
        when(zonas.cobertura(anyDouble(), anyDouble())).thenReturn(new CoberturaVista(false, null));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(CIUDADANO, nuevo(null), List.of(), IP, "JUnit"));

        assertEquals("FUERA_DE_COBERTURA", error.getCodigo());
        verify(reportes, never()).save(any());
    }

    @Test
    void masDeTresFotografiasSeRechazan() {
        MultipartFile foto = new MockMultipartFile("fotos", "f.jpg", "image/jpeg", new byte[]{1});
        List<MultipartFile> cuatro = Collections.nCopies(4, foto);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(CIUDADANO, nuevo(null), cuatro, IP, "JUnit"));

        assertEquals(400, error.getEstadoHttp());
        verify(archivos, never()).guardarFotosCiudadanas(anyLong(), any());
    }

    @Test
    void elLimiteSeRevisaAntesDeGuardar() {
        org.mockito.Mockito.doThrow(new mx.uv.taan.comun.error.LimiteExcedidoException(3600, "x"))
                .when(envios).asegurarDentroDelLimite(eq(IP), eq(CIUDADANO), anyString());

        assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(CIUDADANO, nuevo(null), List.of(), IP, "JUnit"));

        verify(reportes, never()).save(any());
    }

    private static NuevoReporte nuevo(String campoAuxiliar) {
        return new NuevoReporte("Hay un bache enorme frente a la escuela primaria", 18.85, -97.06,
                OrigenReporte.web, campoAuxiliar);
    }

    private static CondicionCiudadano cuentaEnRegla() {
        return new CondicionCiudadano(true, false, false, null, null);
    }
}
