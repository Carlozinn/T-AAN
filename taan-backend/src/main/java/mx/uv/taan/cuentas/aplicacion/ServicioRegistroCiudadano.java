package mx.uv.taan.cuentas.aplicacion;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.comun.seguridad.JwtTaan;
import mx.uv.taan.cuentas.dominio.Ciudadano;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;
import mx.uv.taan.cuentas.infraestructura.LimiteSolicitudesCuenta;
import mx.uv.taan.cuentas.infraestructura.ServicioTokens;

/**
 * Registro de cuentas ciudadanas con correo y su verificación (CU-24; RF-001, RNF-022).
 */
@Service
public class ServicioRegistroCiudadano {

    private static final Logger log = LoggerFactory.getLogger(ServicioRegistroCiudadano.class);

    private final CiudadanoRepository ciudadanos;
    private final PasswordEncoder passwordEncoder;
    private final VerificadorCaptcha captcha;
    private final EnviadorCorreo correo;
    private final ServicioTokens tokens;
    private final LimiteSolicitudesCuenta limite;
    private final String urlWeb;

    public ServicioRegistroCiudadano(CiudadanoRepository ciudadanos, PasswordEncoder passwordEncoder,
                                     VerificadorCaptcha captcha, EnviadorCorreo correo, ServicioTokens tokens,
                                     LimiteSolicitudesCuenta limite,
                                     @Value("${taan.web.url:http://localhost:4200}") String urlWeb) {
        this.ciudadanos = ciudadanos;
        this.passwordEncoder = passwordEncoder;
        this.captcha = captcha;
        this.correo = correo;
        this.tokens = tokens;
        this.limite = limite;
        this.urlWeb = urlWeb;
    }

    /**
     * Crea la cuenta (sin verificar) y envía el enlace. El honeypot responde 202 sin crear
     * nada, para no darle pistas al bot (RNF-022). Antes de todo se aplica el límite por origen (429).
     */
    @Transactional
    public void registrar(String nombre, String correoSolicitado, String password,
                          String captchaToken, String campoAuxiliar, String origenIp) {
        limite.registrar(origenIp, LimiteSolicitudesCuenta.Tipo.registro);
        if (campoAuxiliar != null && !campoAuxiliar.isBlank()) {
            log.info("Registro descartado por honeypot.");
            return;
        }
        if (!captcha.esValido(captchaToken)) {
            throw new ReglaNegocioException(400, "CAPTCHA_INVALIDO",
                    "No pudimos confirmar que no eres un robot. Inténtalo de nuevo.");
        }
        String correoNormalizado = normalizar(correoSolicitado);
        if (ciudadanos.findByCorreo(correoNormalizado).isPresent()) {
            throw new ReglaNegocioException(409, "CORREO_REGISTRADO",
                    "Ese correo ya tiene una cuenta. Inicia sesión o recupera tu acceso.");
        }
        Ciudadano cuenta = ciudadanos.save(
                Ciudadano.conCorreo(nombre.strip(), correoNormalizado, passwordEncoder.encode(password)));
        enviarEnlace(cuenta);
    }

    /** Confirma el enlace (CU-24). Enlace inválido o caducado: 422 ENLACE_VENCIDO (EX-02). */
    @Transactional
    public void verificar(String token) {
        ServicioTokens.TokenValido valido = tokens.validar(token, JwtTaan.TIPO_VERIFICACION)
                .orElseThrow(() -> new ReglaNegocioException(422, "ENLACE_VENCIDO",
                        "El enlace ya no es válido. Solicita uno nuevo."));
        // Si la cuenta ya no existe no hay nada que confirmar; verificar dos veces es inofensivo.
        ciudadanos.findById(valido.id()).ifPresent(Ciudadano::verificarCorreo);
    }

    /** Reenvía el enlace. Siempre responde igual, exista o no la cuenta (no revela correos). */
    @Transactional(readOnly = true)
    public void reenviar(String correoSolicitado, String origenIp) {
        limite.registrar(origenIp, LimiteSolicitudesCuenta.Tipo.reenvio_verificacion);
        ciudadanos.findByCorreo(normalizar(correoSolicitado))
                .filter(c -> !c.esDeGoogle() && !c.isCorreoVerificado())
                .ifPresent(this::enviarEnlace);
    }

    private void enviarEnlace(Ciudadano cuenta) {
        String enlace = urlWeb + "/verificacion?token=" + tokens.emitirVerificacion(cuenta.getId());
        correo.enviarVerificacion(cuenta.getCorreo(), cuenta.getNombre(), enlace);
    }

    static String normalizar(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }
}
