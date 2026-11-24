package mx.uv.taan.cuentas.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.dominio.Ciudadano;
import mx.uv.taan.comun.error.LimiteExcedidoException;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;
import mx.uv.taan.cuentas.infraestructura.LimiteSolicitudesCuenta;
import mx.uv.taan.cuentas.infraestructura.ServicioTokens;

class ServicioRegistroCiudadanoTest {

    private static final String IP = "203.0.113.5";

    private CiudadanoRepository ciudadanos;
    private VerificadorCaptcha captcha;
    private EnviadorCorreo correo;
    private ServicioTokens tokens;
    private LimiteSolicitudesCuenta limite;
    private ServicioRegistroCiudadano servicio;

    @BeforeEach
    void preparar() {
        ciudadanos = mock(CiudadanoRepository.class);
        captcha = mock(VerificadorCaptcha.class);
        correo = mock(EnviadorCorreo.class);
        tokens = mock(ServicioTokens.class);
        limite = mock(LimiteSolicitudesCuenta.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("[hash]");
        when(captcha.esValido(anyString())).thenReturn(true);
        when(ciudadanos.save(any())).thenAnswer(invocacion -> {
            Ciudadano cuenta = invocacion.getArgument(0);
            ReflectionTestUtils.setField(cuenta, "id", 9L);
            return cuenta;
        });
        servicio = new ServicioRegistroCiudadano(ciudadanos, passwordEncoder, captcha, correo, tokens, limite,
                "http://localhost:4200");
    }

    @Test
    void registraNormalizandoElCorreoYEnviaElEnlace() {
        when(ciudadanos.findByCorreo("vecina@correo.mx")).thenReturn(Optional.empty());
        when(tokens.emitirVerificacion(9L)).thenReturn("token-firmado");

        servicio.registrar("Vecina Ejemplo", "  Vecina@Correo.MX ", "contrasena8", "captcha", null, IP);

        verify(ciudadanos).save(any());
        verify(correo).enviarVerificacion(eq("vecina@correo.mx"), eq("Vecina Ejemplo"), contains("token-firmado"));
    }

    @Test
    void elLimitePorOrigenDetieneElRegistroAntesDeCrearNada() {
        doThrow(new LimiteExcedidoException(3600)).when(limite).registrar(IP, LimiteSolicitudesCuenta.Tipo.registro);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar("Vecina", "vecina@correo.mx", "contrasena8", "captcha", null, IP));

        assertEquals(429, error.getEstadoHttp());
        verify(ciudadanos, never()).save(any());
        verify(correo, never()).enviarVerificacion(anyString(), anyString(), anyString());
    }

    @Test
    void elReenvioCuentaContraElLimiteAunqueElCorreoNoExista() {
        when(ciudadanos.findByCorreo("nadie@correo.mx")).thenReturn(Optional.empty());

        servicio.reenviar("nadie@correo.mx", IP);

        verify(limite).registrar(IP, LimiteSolicitudesCuenta.Tipo.reenvio_verificacion);
        verify(correo, never()).enviarVerificacion(anyString(), anyString(), anyString());
    }

    @Test
    void elCorreoDuplicadoResponde409() {
        when(ciudadanos.findByCorreo("vecina@correo.mx"))
                .thenReturn(Optional.of(Ciudadano.conCorreo("Otra", "vecina@correo.mx", "[hash]")));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar("Vecina", "vecina@correo.mx", "contrasena8", "captcha", null, IP));

        assertEquals("CORREO_REGISTRADO", error.getCodigo());
        assertEquals(409, error.getEstadoHttp());
        verify(ciudadanos, never()).save(any());
    }

    @Test
    void elHoneypotDescartaElRegistroSinAvisarle() {
        servicio.registrar("Bot", "bot@correo.mx", "contrasena8", "captcha", "relleno-de-bot", IP);

        verify(ciudadanos, never()).save(any());
        verify(correo, never()).enviarVerificacion(anyString(), anyString(), anyString());
    }

    @Test
    void elCaptchaInvalidoDetieneElRegistro() {
        when(captcha.esValido("malo")).thenReturn(false);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar("Vecina", "vecina@correo.mx", "contrasena8", "malo", null, IP));

        assertEquals("CAPTCHA_INVALIDO", error.getCodigo());
    }

    @Test
    void unEnlaceVencidoResponde422() {
        when(tokens.validar(anyString(), anyString())).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.verificar("token-caducado"));

        assertEquals("ENLACE_VENCIDO", error.getCodigo());
        assertEquals(422, error.getEstadoHttp());
    }

    @Test
    void verificarMarcaElCorreoComoConfirmado() {
        Ciudadano cuenta = Ciudadano.conCorreo("Vecina", "vecina@correo.mx", "[hash]");
        when(tokens.validar(eq("token-bueno"), anyString()))
                .thenReturn(Optional.of(new ServicioTokens.TokenValido(9L, mx.uv.taan.cuentas.dominio.Rol.ciudadano)));
        when(ciudadanos.findById(anyLong())).thenReturn(Optional.of(cuenta));

        servicio.verificar("token-bueno");

        assertEquals(true, cuenta.isCorreoVerificado());
    }
}
