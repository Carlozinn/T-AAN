package mx.uv.taan.cuentas.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.comun.error.LimiteExcedidoException;
import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.dominio.Ciudadano;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;
import mx.uv.taan.cuentas.infraestructura.LimiteSolicitudesCuenta;
import mx.uv.taan.cuentas.infraestructura.ServicioTokens;

/** Usa el ServicioTokens real para probar que el enlace es de un solo uso. */
class ServicioRecuperacionPasswordTest {

    private static final String IP = "203.0.113.5";

    private CiudadanoRepository ciudadanos;
    private PasswordEncoder passwordEncoder;
    private EnviadorCorreo correo;
    private LimiteSolicitudesCuenta limite;
    private ServicioRecuperacionPassword servicio;
    private Ciudadano cuenta;

    @BeforeEach
    void preparar() {
        ciudadanos = mock(CiudadanoRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        correo = mock(EnviadorCorreo.class);
        limite = mock(LimiteSolicitudesCuenta.class);
        ServicioTokens tokens = new ServicioTokens(new SecretKeySpec(
                "clave-de-pruebas-con-al-menos-32-caracteres!".getBytes(StandardCharsets.UTF_8), "HmacSHA256"),
                15, 30, 24, 60);
        servicio = new ServicioRecuperacionPassword(ciudadanos, passwordEncoder, tokens, correo, limite,
                "http://localhost:4200");

        cuenta = Ciudadano.conCorreo("Vecina", "vecina@correo.mx", "[hash-original]");
        ReflectionTestUtils.setField(cuenta, "id", 4L);
        when(ciudadanos.findByCorreo("vecina@correo.mx")).thenReturn(Optional.of(cuenta));
        when(ciudadanos.findById(4L)).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.encode(anyString())).thenReturn("[hash-nuevo]");
    }

    @Test
    void elEnlaceSirveUnaSolaVez() {
        String token = tokenEnviado();

        servicio.restablecer(token, "NuevaSegura1");
        assertEquals("[hash-nuevo]", cuenta.getPasswordHash());
        assertTrue(cuenta.isCorreoVerificado());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.restablecer(token, "OtraMas123"));
        assertEquals("ENLACE_VENCIDO", error.getCodigo());
        assertEquals(422, error.getEstadoHttp());
    }

    @Test
    void unCorreoInexistenteOGoogleNoEnviaNadaYNoFalla() {
        when(ciudadanos.findByCorreo("google@correo.mx"))
                .thenReturn(Optional.of(Ciudadano.conGoogle("G", "google@correo.mx", "sub-1")));

        servicio.solicitar("nadie@correo.mx", IP);
        servicio.solicitar("google@correo.mx", IP);

        verify(correo, never()).enviarRecuperacion(anyString(), anyString(), anyString());
        verify(limite, times(2)).registrar(IP, LimiteSolicitudesCuenta.Tipo.recuperacion);
    }

    @Test
    void elLimitePorOrigenDetieneLaRecuperacionSinEnviarCorreo() {
        doThrow(new LimiteExcedidoException(3600)).when(limite).registrar(IP, LimiteSolicitudesCuenta.Tipo.recuperacion);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.solicitar("vecina@correo.mx", IP));

        assertEquals("LIMITE_EXCEDIDO", error.getCodigo());
        verify(correo, never()).enviarRecuperacion(anyString(), anyString(), anyString());
    }

    @Test
    void unTokenCorruptoResponde422() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.restablecer("no-es-un-token", "NuevaSegura1"));
        assertEquals("ENLACE_VENCIDO", error.getCodigo());
    }

    private String tokenEnviado() {
        servicio.solicitar(" Vecina@Correo.mx ", IP);
        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correo).enviarRecuperacion(org.mockito.ArgumentMatchers.eq("vecina@correo.mx"),
                anyString(), enlace.capture());
        return enlace.getValue().substring(enlace.getValue().indexOf("token=") + "token=".length());
    }
}
