package mx.uv.taan.cuentas.infraestructura;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import mx.uv.taan.cuentas.aplicacion.VerificadorCaptcha;

/**
 * Verifica el token del CAPTCHA con Cloudflare Turnstile (RNF-022).
 *
 * <p>Sin TURNSTILE_SECRETO configurado (desarrollo) acepta todo y lo avisa en el log.
 * Si Turnstile no responde, se rechaza el intento (mejor pedir reintentar que abrir la
 * puerta a los bots).</p>
 */
public class CaptchaTurnstile implements VerificadorCaptcha {

    private static final Logger log = LoggerFactory.getLogger(CaptchaTurnstile.class);
    private static final String URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final RestClient rest;
    private final String secreto;

    public CaptchaTurnstile(RestClient rest, String secreto) {
        this.rest = rest;
        this.secreto = secreto == null ? "" : secreto;
    }

    @Override
    public boolean esValido(String captchaToken) {
        if (secreto.isBlank()) {
            log.warn("[solo desarrollo] TURNSTILE_SECRETO vacío: el CAPTCHA se da por válido.");
            return true;
        }
        if (captchaToken == null || captchaToken.isBlank()) {
            return false;
        }
        try {
            MultiValueMap<String, String> cuerpo = new LinkedMultiValueMap<>();
            cuerpo.add("secret", secreto);
            cuerpo.add("response", captchaToken);
            Map<?, ?> respuesta = rest.post()
                    .uri(URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);
            return respuesta != null && Boolean.TRUE.equals(respuesta.get("success"));
        } catch (Exception e) {
            log.error("No se pudo consultar Turnstile: {}", e.getMessage());
            return false;
        }
    }
}
