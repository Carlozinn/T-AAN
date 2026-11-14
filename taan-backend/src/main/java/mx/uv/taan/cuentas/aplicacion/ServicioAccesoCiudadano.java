package mx.uv.taan.cuentas.aplicacion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.dominio.Ciudadano;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;
import mx.uv.taan.cuentas.infraestructura.RegistroIntentosAcceso;
import mx.uv.taan.cuentas.infraestructura.ServicioTokens;

/**
 * Inicio de sesión ciudadano con correo o Google y cambio de contraseña (CU-04, CU-25;
 * RF-002, RNF-009).
 *
 * <p>Una cuenta suspendida sí inicia sesión: puede consultar «Mis reportes»; crear o
 * reabrir se bloquea en el módulo de reportes con CUENTA_SUSPENDIDA (RF-043).</p>
 */
@Service
public class ServicioAccesoCiudadano {

    private final CiudadanoRepository ciudadanos;
    private final PasswordEncoder passwordEncoder;
    private final ServicioTokens tokens;
    private final RegistroIntentosAcceso intentos;
    private final VerificadorGoogle google;
    private final VerificadorCaptcha captcha;

    public ServicioAccesoCiudadano(CiudadanoRepository ciudadanos, PasswordEncoder passwordEncoder,
                                   ServicioTokens tokens, RegistroIntentosAcceso intentos,
                                   VerificadorGoogle google, VerificadorCaptcha captcha) {
        this.ciudadanos = ciudadanos;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.intentos = intentos;
        this.google = google;
        this.captcha = captcha;
    }

    /** Credenciales incorrectas responden 400 sin revelar si el correo existe (contrato). */
    @Transactional
    public SesionEmitida iniciarSesion(String correo, String password, String origenIp) {
        Ciudadano cuenta = ciudadanos.findByCorreo(ServicioRegistroCiudadano.normalizar(correo)).orElse(null);
        intentos.asegurarDentroDelLimite(origenIp, cuenta == null ? null : cuenta.getId(), null);
        if (cuenta == null || cuenta.esDeGoogle()
                || !passwordEncoder.matches(password, cuenta.getPasswordHash())) {
            intentos.registrarFallido(origenIp, cuenta == null ? null : cuenta.getId(), null);
            throw credencialesInvalidas();
        }
        intentos.registrarExitoso(origenIp, cuenta.getId(), null);
        return sesionDe(cuenta);
    }

    /**
     * Continuar con Google (CU-24, CU-04): si el correo no tiene cuenta, la crea; crear
     * exige captchaToken y aceptar el aviso de privacidad. requiereTelefono avisa al
     * frontend que falta el teléfono antes de reportar (FA-03).
     */
    @Transactional
    public SesionEmitida accederConGoogle(String idToken, String captchaToken, Boolean aceptaAvisoPrivacidad) {
        VerificadorGoogle.IdentidadGoogle identidad = google.verificar(idToken);
        Ciudadano existente = ciudadanos.findByGoogleSub(identidad.sub()).orElse(null);
        if (existente != null) {
            return sesionDe(existente);
        }
        String correo = ServicioRegistroCiudadano.normalizar(identidad.correo());
        if (ciudadanos.findByCorreo(correo).isPresent()) {
            // El contrato solo define 200/400/429 aquí, por eso el conflicto responde 400.
            throw new ReglaNegocioException(400, "CORREO_REGISTRADO",
                    "Ese correo ya tiene una cuenta con contraseña. Inicia sesión con tu correo.");
        }
        if (!Boolean.TRUE.equals(aceptaAvisoPrivacidad)) {
            throw new ReglaNegocioException(400, "AVISO_PRIVACIDAD_REQUERIDO",
                    "Para crear tu cuenta necesitas aceptar el aviso de privacidad.");
        }
        if (!captcha.esValido(captchaToken)) {
            throw new ReglaNegocioException(400, "CAPTCHA_INVALIDO",
                    "No pudimos confirmar que no eres un robot. Inténtalo de nuevo.");
        }
        String nombre = identidad.nombre() == null || identidad.nombre().isBlank()
                ? correo : identidad.nombre().strip();
        return sesionDe(ciudadanos.save(Ciudadano.conGoogle(nombre, correo, identidad.sub())));
    }

    /** Cambio de contraseña (CU-25). Las cuentas Google responden 422 (FA-01). */
    @Transactional
    public void cambiarPassword(long ciudadanoId, String passwordActual, String nuevaPassword) {
        Ciudadano cuenta = ciudadanos.findById(ciudadanoId)
                .orElseThrow(() -> new ReglaNegocioException(401, "NO_AUTENTICADO", "Vuelve a iniciar sesión."));
        if (cuenta.esDeGoogle()) {
            throw new ReglaNegocioException(422, "METODO_GOOGLE",
                    "Tu cuenta entra con Google y no usa contraseña de T'AAN.");
        }
        if (!passwordEncoder.matches(passwordActual, cuenta.getPasswordHash())) {
            throw new ReglaNegocioException(400, "PASSWORD_ACTUAL_INCORRECTA",
                    "La contraseña actual no coincide.");
        }
        cuenta.cambiarPasswordHash(passwordEncoder.encode(nuevaPassword));
    }

    private SesionEmitida sesionDe(Ciudadano cuenta) {
        return SesionEmitida.paraCiudadano(
                tokens.emitirSesion(cuenta.getId(), Rol.ciudadano), cuenta.requiereTelefono());
    }

    static ReglaNegocioException credencialesInvalidas() {
        return new ReglaNegocioException(400, "CREDENCIALES_INVALIDAS", "Revisa tu correo y contraseña.");
    }
}
