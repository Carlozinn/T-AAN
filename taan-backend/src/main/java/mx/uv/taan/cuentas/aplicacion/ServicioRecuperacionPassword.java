package mx.uv.taan.cuentas.aplicacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.dominio.Ciudadano;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;
import mx.uv.taan.cuentas.infraestructura.LimiteSolicitudesCuenta;
import mx.uv.taan.cuentas.infraestructura.ServicioTokens;

/**
 * Recuperación de la contraseña ciudadana con un enlace por correo (CU-04, RF-002).
 *
 * <p>El enlace es un JWT con vigencia corta que guarda una huella de la contraseña vigente:
 * en cuanto se restablece, la huella cambia y el mismo enlace ya no sirve. Así es de un solo
 * uso sin necesidad de una tabla de tokens.</p>
 */
@Service
public class ServicioRecuperacionPassword {

    private final CiudadanoRepository ciudadanos;
    private final PasswordEncoder passwordEncoder;
    private final ServicioTokens tokens;
    private final EnviadorCorreo correo;
    private final LimiteSolicitudesCuenta limite;
    private final String urlWeb;

    public ServicioRecuperacionPassword(CiudadanoRepository ciudadanos, PasswordEncoder passwordEncoder,
                                        ServicioTokens tokens, EnviadorCorreo correo, LimiteSolicitudesCuenta limite,
                                        @Value("${taan.web.url:http://localhost:4200}") String urlWeb) {
        this.ciudadanos = ciudadanos;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.correo = correo;
        this.limite = limite;
        this.urlWeb = urlWeb;
    }

    /**
     * Siempre responde igual, exista o no la cuenta; las cuentas Google no tienen contraseña.
     * El límite por origen (429) se aplica antes de buscar la cuenta.
     */
    @Transactional(readOnly = true)
    public void solicitar(String correoSolicitado, String origenIp) {
        limite.registrar(origenIp, LimiteSolicitudesCuenta.Tipo.recuperacion);
        ciudadanos.findByCorreo(ServicioRegistroCiudadano.normalizar(correoSolicitado))
                .filter(cuenta -> !cuenta.esDeGoogle())
                .ifPresent(cuenta -> {
                    String token = tokens.emitirRecuperacion(cuenta.getId(), huella(cuenta.getPasswordHash()));
                    correo.enviarRecuperacion(cuenta.getCorreo(), cuenta.getNombre(),
                            urlWeb + "/recuperacion?token=" + token);
                });
    }

    /** Enlace inválido, caducado o ya usado: 422 ENLACE_VENCIDO. */
    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        ServicioTokens.TokenRecuperacion valido = tokens.validarRecuperacion(token)
                .orElseThrow(ServicioRecuperacionPassword::enlaceVencido);
        Ciudadano cuenta = ciudadanos.findById(valido.id())
                .filter(c -> !c.esDeGoogle())
                .filter(c -> coincide(huella(c.getPasswordHash()), valido.huella()))
                .orElseThrow(ServicioRecuperacionPassword::enlaceVencido);
        cuenta.restablecerPassword(passwordEncoder.encode(nuevaPassword));
    }

    /** SHA-256 recortado del hash BCrypt: no revela nada útil y cambia con cada contraseña. */
    static String huella(String passwordHash) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(resumen).substring(0, 22);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Comparación en tiempo constante. */
    private static boolean coincide(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static ReglaNegocioException enlaceVencido() {
        return new ReglaNegocioException(422, "ENLACE_VENCIDO",
                "El enlace ya no es válido. Solicita uno nuevo.");
    }
}
