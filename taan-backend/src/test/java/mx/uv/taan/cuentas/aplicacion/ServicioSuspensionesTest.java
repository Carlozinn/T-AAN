package mx.uv.taan.cuentas.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.dominio.Ciudadano;
import mx.uv.taan.cuentas.dominio.EstadoCuenta;
import mx.uv.taan.cuentas.dominio.LevantamientoSuspension;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;
import mx.uv.taan.cuentas.infraestructura.LevantamientoSuspensionRepository;

class ServicioSuspensionesTest {

    private CiudadanoRepository ciudadanos;
    private LevantamientoSuspensionRepository levantamientos;
    private EnviadorCorreo correo;
    private ServicioSuspensiones servicio;
    private Ciudadano cuenta;

    @BeforeEach
    void preparar() {
        ciudadanos = mock(CiudadanoRepository.class);
        levantamientos = mock(LevantamientoSuspensionRepository.class);
        correo = mock(EnviadorCorreo.class);
        servicio = new ServicioSuspensiones(ciudadanos, levantamientos, correo);
        cuenta = Ciudadano.conCorreo("Vecino", "vecino@correo.mx", "[hash]");
        when(ciudadanos.findById(7L)).thenReturn(Optional.of(cuenta));
    }

    @Test
    void suspendeLaCuentaYAvisaConMotivoYVigencia() {
        Instant hasta = Instant.parse("2026-12-01T18:00:00Z");

        assertTrue(servicio.suspender(7L, "Reportes falsos reiterados.", hasta));

        assertEquals(EstadoCuenta.suspendida, cuenta.getEstado());
        assertEquals("Reportes falsos reiterados.", cuenta.getMotivoSuspension());
        assertEquals(hasta, cuenta.getSuspendidaHasta());
        verify(correo).enviarAvisoSuspension("vecino@correo.mx", "Vecino", "Reportes falsos reiterados.",
                "hasta el 01/12/2026 12:00");
    }

    @Test
    void laSuspensionIndefinidaSeAvisaComoTal() {
        servicio.suspender(7L, "Reincide con reportes falsos.", null);

        assertNull(cuenta.getSuspendidaHasta());
        verify(correo).enviarAvisoSuspension(anyString(), anyString(), anyString(), eq("por tiempo indefinido"));
    }

    @Test
    void unFalloDelCorreoNoDeshaceLaSuspension() {
        doThrow(new IllegalStateException("SMTP caído")).when(correo)
                .enviarAvisoSuspension(anyString(), anyString(), anyString(), anyString());

        assertTrue(servicio.suspender(7L, "Reportes falsos reiterados.", null));
        assertEquals(EstadoCuenta.suspendida, cuenta.getEstado());
    }

    @Test
    void unaCuentaQueYaNoExisteNoSeSuspende() {
        assertFalse(servicio.suspender(99L, "Reportes falsos reiterados.", null));
    }

    @Test
    void laSuspensionDejaDeEstarVigenteAlVencer() {
        cuenta.suspender("Reportes falsos reiterados.", Instant.now().minus(Duration.ofMinutes(1)));
        assertFalse(cuenta.suspensionVigente(Instant.now()));

        cuenta.suspender("Reincide con reportes falsos.", null);
        assertTrue(cuenta.suspensionVigente(Instant.now()));
    }

    // ------------------------------------------------------------------ levantamiento

    @Test
    void levantarReactivaLaCuentaRegistraLaAuditoriaYAvisa() {
        Instant hasta = Instant.now().plus(Duration.ofDays(3));
        cuenta.suspender("Reportes falsos reiterados.", hasta);

        servicio.levantar(1L, 7L, "  Se aclaró que los reportes eran reales.  ");

        assertEquals(EstadoCuenta.activa, cuenta.getEstado());
        assertNull(cuenta.getMotivoSuspension());
        assertNull(cuenta.getSuspendidaHasta());
        ArgumentCaptor<LevantamientoSuspension> registro = ArgumentCaptor.forClass(LevantamientoSuspension.class);
        verify(levantamientos).save(registro.capture());
        assertEquals(1L, registro.getValue().getLevantadoPor());
        assertEquals("Se aclaró que los reportes eran reales.", registro.getValue().getMotivo());
        assertEquals("Reportes falsos reiterados.", registro.getValue().getMotivoSuspensionAnterior());
        assertEquals(hasta, registro.getValue().getSuspendidaHastaAnterior());
        verify(correo).enviarAvisoLevantamiento("vecino@correo.mx", "Vecino", "Se aclaró que los reportes eran reales.");
    }

    @Test
    void levantarUnaSuspensionIndefinidaGuardaQueEraIndefinida() {
        cuenta.suspender("Reincide con reportes falsos.", null);

        servicio.levantar(1L, 7L, "Se aclaró que los reportes eran reales.");

        ArgumentCaptor<LevantamientoSuspension> registro = ArgumentCaptor.forClass(LevantamientoSuspension.class);
        verify(levantamientos).save(registro.capture());
        assertNull(registro.getValue().getSuspendidaHastaAnterior());
        assertFalse(cuenta.suspensionVigente(Instant.now()));
    }

    @Test
    void unaCuentaActivaResponde409() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.levantar(1L, 7L, "Se aclaró que los reportes eran reales."));

        assertEquals(409, error.getEstadoHttp());
        assertEquals("CUENTA_NO_SUSPENDIDA", error.getCodigo());
        verify(levantamientos, never()).save(any());
        verify(correo, never()).enviarAvisoLevantamiento(anyString(), anyString(), anyString());
    }

    @Test
    void unaSuspensionVencidaCuentaComoNoSuspendida() {
        cuenta.suspender("Reportes falsos reiterados.", Instant.now().minus(Duration.ofHours(1)));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.levantar(1L, 7L, "Se aclaró que los reportes eran reales."));

        assertEquals("CUENTA_NO_SUSPENDIDA", error.getCodigo());
    }

    @Test
    void unCiudadanoInexistenteResponde404() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.levantar(1L, 99L, "Se aclaró que los reportes eran reales."));

        assertEquals(404, error.getEstadoHttp());
    }

    @Test
    void unFalloDelCorreoNoDeshaceElLevantamiento() {
        cuenta.suspender("Reportes falsos reiterados.", null);
        doThrow(new IllegalStateException("SMTP caído")).when(correo)
                .enviarAvisoLevantamiento(anyString(), anyString(), anyString());

        servicio.levantar(1L, 7L, "Se aclaró que los reportes eran reales.");

        assertEquals(EstadoCuenta.activa, cuenta.getEstado());
    }
}
