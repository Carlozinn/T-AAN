package mx.uv.taan.cuentas.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import mx.uv.taan.comun.seguridad.JwtTaan;
import mx.uv.taan.cuentas.dominio.Rol;

/** Ida y vuelta real de los tokens: emitir con Nimbus y validar firma, tipo y claims. */
class ServicioTokensTest {

    private static final SecretKeySpec CLAVE = new SecretKeySpec(
            "clave-de-pruebas-con-al-menos-32-caracteres!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final ServicioTokens tokens = new ServicioTokens(CLAVE, 15, 30, 24, 60);

    @Test
    void elAccesoYElRefrescoConservanIdentidadYRol() {
        ServicioTokens.ParDeTokens par = tokens.emitirSesion(42, Rol.coordinacion);

        ServicioTokens.TokenValido acceso = tokens.validar(par.accessToken(), JwtTaan.TIPO_ACCESO).orElseThrow();
        ServicioTokens.TokenValido refresco = tokens.validar(par.refreshToken(), JwtTaan.TIPO_REFRESCO).orElseThrow();

        assertEquals(42, acceso.id());
        assertEquals(Rol.coordinacion, acceso.rol());
        assertEquals(42, refresco.id());
        assertEquals(15 * 60, par.expiraEn());
    }

    @Test
    void unTokenDeOtroTipoNoSeAcepta() {
        ServicioTokens.ParDeTokens par = tokens.emitirSesion(7, Rol.ciudadano);

        // Un refresco no sirve como acceso ni al revés; una verificación tampoco renueva sesión.
        assertTrue(tokens.validar(par.refreshToken(), JwtTaan.TIPO_ACCESO).isEmpty());
        assertTrue(tokens.validar(par.accessToken(), JwtTaan.TIPO_REFRESCO).isEmpty());
        assertTrue(tokens.validar(tokens.emitirVerificacion(7), JwtTaan.TIPO_REFRESCO).isEmpty());
    }

    @Test
    void unTokenAjenoOCorruptoSeRechazaSinExcepcion() {
        assertTrue(tokens.validar("no-es-un-jwt", JwtTaan.TIPO_ACCESO).isEmpty());

        ServicioTokens otraClave = new ServicioTokens(new SecretKeySpec(
                "otra-clave-distinta-tambien-de-32-caracteres".getBytes(StandardCharsets.UTF_8), "HmacSHA256"),
                15, 30, 24, 60);
        String ajeno = otraClave.emitirSesion(1, Rol.ciudadano).accessToken();
        assertTrue(tokens.validar(ajeno, JwtTaan.TIPO_ACCESO).isEmpty());
    }

    @Test
    void elEnlaceDeRecuperacionConservaSuHuellaYNoSirveComoOtroTipo() {
        String enlace = tokens.emitirRecuperacion(3, "huella-abc");

        ServicioTokens.TokenRecuperacion valido = tokens.validarRecuperacion(enlace).orElseThrow();
        assertEquals(3, valido.id());
        assertEquals("huella-abc", valido.huella());
        assertTrue(tokens.validar(enlace, JwtTaan.TIPO_ACCESO).isEmpty());
        assertTrue(tokens.validarRecuperacion(tokens.emitirVerificacion(3)).isEmpty());
    }
}
