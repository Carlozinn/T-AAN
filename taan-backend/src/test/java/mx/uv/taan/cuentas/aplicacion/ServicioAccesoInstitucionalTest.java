package mx.uv.taan.cuentas.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.cuentas.dominio.Usuario;
import mx.uv.taan.cuentas.infraestructura.RegistroIntentosAcceso;
import mx.uv.taan.cuentas.infraestructura.ServicioTokens;
import mx.uv.taan.cuentas.infraestructura.UsuarioRepository;

class ServicioAccesoInstitucionalTest {

    private static final String IP = "127.0.0.1";

    private UsuarioRepository usuarios;
    private PasswordEncoder passwordEncoder;
    private RegistroIntentosAcceso intentos;
    private ServicioAccesoInstitucional servicio;

    private Usuario coordinacion;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        intentos = mock(RegistroIntentosAcceso.class);
        ServicioTokens tokens = mock(ServicioTokens.class);
        when(tokens.emitirSesion(anyLong(), any()))
                .thenReturn(new ServicioTokens.ParDeTokens("acceso", "refresco", 900));
        servicio = new ServicioAccesoInstitucional(usuarios, passwordEncoder, tokens, intentos);

        coordinacion = Usuario.conCredencialTemporal(
                "Coordinación", "coordinacion@ixtaczoquitlan.gob.mx", "[hash]", Rol.coordinacion, 1L, null);
        ReflectionTestUtils.setField(coordinacion, "id", 5L);
    }

    @Test
    void conCredencialTemporalLaSesionExigeCambiarPassword() {
        when(usuarios.findByCorreo("coordinacion@ixtaczoquitlan.gob.mx")).thenReturn(Optional.of(coordinacion));
        when(passwordEncoder.matches("temporal123", "[hash]")).thenReturn(true);

        SesionEmitida sesion = servicio.iniciarSesion("Coordinacion@Ixtaczoquitlan.gob.mx ", "temporal123", IP);

        assertEquals(Rol.coordinacion, sesion.rol());
        assertTrue(sesion.requiereCambioPassword());
        verify(intentos).registrarExitoso(IP, null, 5L);
    }

    @Test
    void laPasswordIncorrectaRespondeSinRevelarSiElCorreoExiste() {
        when(usuarios.findByCorreo(anyString())).thenReturn(Optional.of(coordinacion));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.iniciarSesion("coordinacion@ixtaczoquitlan.gob.mx", "incorrecta", IP));

        assertEquals("CREDENCIALES_INVALIDAS", error.getCodigo());
        assertEquals(400, error.getEstadoHttp());
        verify(intentos).registrarFallido(IP, null, 5L);
    }

    @Test
    void unCorreoInexistenteRespondeConElMismoCodigo() {
        when(usuarios.findByCorreo(anyString())).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.iniciarSesion("nadie@taan.mx", "loquesea", IP));

        assertEquals("CREDENCIALES_INVALIDAS", error.getCodigo());
    }

    @Test
    void cambiarPasswordApagaLaCredencialTemporal() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(coordinacion));
        when(passwordEncoder.matches("temporal123", "[hash]")).thenReturn(true);
        when(passwordEncoder.encode("NuevaSegura1")).thenReturn("[hash-nuevo]");

        servicio.cambiarPassword(5L, "temporal123", "NuevaSegura1");

        assertFalse(coordinacion.isCredencialTemporal());
        assertEquals("[hash-nuevo]", coordinacion.getPasswordHash());
    }

    @Test
    void cambiarPasswordExigeLaActualCorrecta() {
        when(usuarios.findById(5L)).thenReturn(Optional.of(coordinacion));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.cambiarPassword(5L, "equivocada", "NuevaSegura1"));

        assertEquals("PASSWORD_ACTUAL_INCORRECTA", error.getCodigo());
        assertTrue(coordinacion.isCredencialTemporal());
    }
}
