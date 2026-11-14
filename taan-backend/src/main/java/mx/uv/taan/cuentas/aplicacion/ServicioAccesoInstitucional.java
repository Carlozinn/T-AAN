package mx.uv.taan.cuentas.aplicacion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.dominio.Usuario;
import mx.uv.taan.cuentas.infraestructura.RegistroIntentosAcceso;
import mx.uv.taan.cuentas.infraestructura.ServicioTokens;
import mx.uv.taan.cuentas.infraestructura.UsuarioRepository;

/**
 * Inicio de sesión del personal institucional y cambio de contraseña (CU-04; RF-002,
 * RF-003, RNF-009). requiereCambioPassword = true obliga a sustituir la credencial
 * temporal en el primer acceso.
 */
@Service
public class ServicioAccesoInstitucional {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final ServicioTokens tokens;
    private final RegistroIntentosAcceso intentos;

    public ServicioAccesoInstitucional(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                                       ServicioTokens tokens, RegistroIntentosAcceso intentos) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.intentos = intentos;
    }

    /** Un usuario desactivado responde igual que una contraseña incorrecta: no se revela. */
    @Transactional
    public SesionEmitida iniciarSesion(String correo, String password, String origenIp) {
        Usuario usuario = usuarios.findByCorreo(ServicioRegistroCiudadano.normalizar(correo)).orElse(null);
        intentos.asegurarDentroDelLimite(origenIp, null, usuario == null ? null : usuario.getId());
        if (usuario == null || !usuario.isActivo()
                || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
            intentos.registrarFallido(origenIp, null, usuario == null ? null : usuario.getId());
            throw ServicioAccesoCiudadano.credencialesInvalidas();
        }
        usuario.registrarAcceso();
        intentos.registrarExitoso(origenIp, null, usuario.getId());
        return SesionEmitida.paraInstitucional(
                tokens.emitirSesion(usuario.getId(), usuario.getRol()),
                usuario.getRol(), usuario.isCredencialTemporal());
    }

    /** Cambio de contraseña (CU-04): valida la actual y apaga la credencial temporal. */
    @Transactional
    public void cambiarPassword(long usuarioId, String passwordActual, String nuevaPassword) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ReglaNegocioException(401, "NO_AUTENTICADO", "Vuelve a iniciar sesión."));
        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new ReglaNegocioException(400, "PASSWORD_ACTUAL_INCORRECTA",
                    "La contraseña actual no coincide.");
        }
        usuario.cambiarPassword(passwordEncoder.encode(nuevaPassword));
    }
}
