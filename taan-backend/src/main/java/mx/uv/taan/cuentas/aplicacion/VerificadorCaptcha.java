package mx.uv.taan.cuentas.aplicacion;

/**
 * Puerto de salida para el CAPTCHA de baja fricción (RNF-022). La implementación real
 * consulta Cloudflare Turnstile; en desarrollo (sin secreto) acepta todo.
 */
public interface VerificadorCaptcha {

    boolean esValido(String captchaToken);
}
