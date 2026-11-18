package mx.uv.taan.cuentas.infraestructura;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import mx.uv.taan.comun.seguridad.JwtTaan;
import mx.uv.taan.cuentas.dominio.Rol;

/**
 * Emite y valida los tokens firmados del backend (RF-002). Todos llevan los claims
 * {@code rol} y {@code tipo} (acceso, refresco, verificacion o recuperacion; {@link JwtTaan}).
 *
 * <p>Los tokens no se guardan en la base: el esquema no tiene tabla de tokens a propósito.
 * Al refrescar, el servicio de sesión vuelve a consultar la cuenta, de modo que una cuenta
 * eliminada o desactivada deja de poder renovarse; el token de acceso caduca solo en minutos.</p>
 *
 * <p>Usa su propio decodificador, sin el filtro de «solo acceso» del recurso protegido
 * (ConfiguracionSeguridad): aquí sí hay que poder leer los demás tipos.</p>
 */
@Component
public class ServicioTokens {

    /** Huella de la contraseña vigente: el enlace de recuperación deja de servir al usarse. */
    private static final String CLAIM_HUELLA = "huella";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration vigenciaAcceso;
    private final Duration vigenciaRefresco;
    private final Duration vigenciaVerificacion;
    private final Duration vigenciaRecuperacion;

    public ServicioTokens(SecretKey claveJwt,
                          @Value("${taan.jwt.minutos-acceso}") long minutosAcceso,
                          @Value("${taan.jwt.dias-refresco}") long diasRefresco,
                          @Value("${taan.jwt.horas-verificacion:24}") long horasVerificacion,
                          @Value("${taan.jwt.minutos-recuperacion:60}") long minutosRecuperacion) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
        this.decoder = NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        this.vigenciaAcceso = Duration.ofMinutes(minutosAcceso);
        this.vigenciaRefresco = Duration.ofDays(diasRefresco);
        this.vigenciaVerificacion = Duration.ofHours(horasVerificacion);
        this.vigenciaRecuperacion = Duration.ofMinutes(minutosRecuperacion);
    }

    /** Par de tokens de una sesión; expiraEn son los segundos de vigencia del acceso (contrato). */
    public record ParDeTokens(String accessToken, String refreshToken, long expiraEn) {
    }

    /** Identidad contenida en un token válido del tipo esperado. */
    public record TokenValido(long id, Rol rol) {
    }

    /** Contenido de un enlace de recuperación válido. */
    public record TokenRecuperacion(long id, String huella) {
    }

    public ParDeTokens emitirSesion(long id, Rol rol) {
        return new ParDeTokens(
                emitir(id, rol, JwtTaan.TIPO_ACCESO, vigenciaAcceso, null),
                emitir(id, rol, JwtTaan.TIPO_REFRESCO, vigenciaRefresco, null),
                vigenciaAcceso.toSeconds());
    }

    /** Token que viaja en el enlace de verificación del correo (CU-24). */
    public String emitirVerificacion(long ciudadanoId) {
        return emitir(ciudadanoId, Rol.ciudadano, JwtTaan.TIPO_VERIFICACION, vigenciaVerificacion, null);
    }

    /**
     * Token del enlace para restablecer la contraseña (CU-04). La huella se calcula sobre la
     * contraseña actual: al cambiarla, cualquier enlace anterior deja de coincidir.
     */
    public String emitirRecuperacion(long ciudadanoId, String huella) {
        return emitir(ciudadanoId, Rol.ciudadano, JwtTaan.TIPO_RECUPERACION, vigenciaRecuperacion, huella);
    }

    /**
     * Valida firma, vigencia y tipo. Regresa vacío si el token no sirve; el código y el
     * estado HTTP (ENLACE_VENCIDO, REFRESCO_INVALIDO...) los decide quien llama.
     */
    public Optional<TokenValido> validar(String token, String tipoEsperado) {
        return decodificar(token, tipoEsperado).map(jwt -> new TokenValido(
                Long.parseLong(jwt.getSubject()),
                Rol.valueOf(jwt.getClaimAsString(JwtTaan.CLAIM_ROL))));
    }

    public Optional<TokenRecuperacion> validarRecuperacion(String token) {
        return decodificar(token, JwtTaan.TIPO_RECUPERACION)
                .filter(jwt -> jwt.getClaimAsString(CLAIM_HUELLA) != null)
                .map(jwt -> new TokenRecuperacion(
                        Long.parseLong(jwt.getSubject()), jwt.getClaimAsString(CLAIM_HUELLA)));
    }

    private Optional<Jwt> decodificar(String token, String tipoEsperado) {
        try {
            Jwt jwt = decoder.decode(token);
            return tipoEsperado.equals(jwt.getClaimAsString(JwtTaan.CLAIM_TIPO))
                    ? Optional.of(jwt) : Optional.empty();
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private String emitir(long id, Rol rol, String tipo, Duration vigencia, String huella) {
        Instant ahora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(Long.toString(id))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vigencia))
                .claim(JwtTaan.CLAIM_ROL, rol.name())
                .claim(JwtTaan.CLAIM_TIPO, tipo);
        if (huella != null) {
            claims.claim(CLAIM_HUELLA, huella);
        }
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims.build())).getTokenValue();
    }
}
