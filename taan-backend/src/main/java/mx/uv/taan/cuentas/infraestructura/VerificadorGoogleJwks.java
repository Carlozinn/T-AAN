package mx.uv.taan.cuentas.infraestructura;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.aplicacion.VerificadorGoogle;

/**
 * Valida el ID token de Google Sign-In (OpenID Connect) contra las llaves públicas de
 * Google: firma, vigencia, emisor y audiencia (el client id de T'AAN). El decodificador
 * se crea hasta el primer uso para no tocar la red al arrancar.
 */
public class VerificadorGoogleJwks implements VerificadorGoogle {

    private static final String JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> EMISORES = List.of("https://accounts.google.com", "accounts.google.com");

    private final String clientId;
    private volatile JwtDecoder decoder;

    public VerificadorGoogleJwks(String clientId) {
        this.clientId = clientId == null ? "" : clientId;
    }

    @Override
    public IdentidadGoogle verificar(String idToken) {
        if (clientId.isBlank()) {
            throw new ReglaNegocioException(400, "GOOGLE_NO_DISPONIBLE",
                    "El acceso con Google aún no está habilitado.");
        }
        try {
            Jwt jwt = decoder().decode(idToken);
            if (!Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
                throw new JwtException("Correo sin verificar en Google");
            }
            return new IdentidadGoogle(jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("name"));
        } catch (JwtException e) {
            throw new ReglaNegocioException(400, "TOKEN_GOOGLE_INVALIDO",
                    "No pudimos validar el acceso con Google. Inténtalo de nuevo.");
        }
    }

    private JwtDecoder decoder() {
        if (decoder == null) {
            synchronized (this) {
                if (decoder == null) {
                    NimbusJwtDecoder nuevo = NimbusJwtDecoder.withJwkSetUri(JWKS).build();
                    nuevo.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                            JwtValidators.createDefault(), emisorYAudiencia()));
                    decoder = nuevo;
                }
            }
        }
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> emisorYAudiencia() {
        return jwt -> {
            boolean emisorValido = jwt.getIssuer() != null && EMISORES.contains(jwt.getIssuer().toString());
            boolean audienciaValida = jwt.getAudience() != null && jwt.getAudience().contains(clientId);
            return emisorValido && audienciaValida
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_token", "Emisor o audiencia incorrectos", null));
        };
    }
}
