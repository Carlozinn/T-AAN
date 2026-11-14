package mx.uv.taan.cuentas.aplicacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.comun.seguridad.JwtTaan;
import mx.uv.taan.cuentas.dominio.Ciudadano;
import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.cuentas.dominio.Usuario;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;
import mx.uv.taan.cuentas.infraestructura.ServicioTokens;
import mx.uv.taan.cuentas.infraestructura.UsuarioRepository;

/**
 * Renovación y cierre de sesión (CU-04; RF-002, RF-031, RF-034).
 *
 * <p>Al refrescar se vuelve a consultar la cuenta: una cuenta ciudadana eliminada o un
 * usuario desactivado dejan de renovarse aunque su refresco siga firmado y vigente.</p>
 */
@Service
public class ServicioSesion {

    private final ServicioTokens tokens;
    private final CiudadanoRepository ciudadanos;
    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;

    public ServicioSesion(ServicioTokens tokens, CiudadanoRepository ciudadanos,
                          UsuarioRepository usuarios, JdbcTemplate jdbc) {
        this.tokens = tokens;
        this.ciudadanos = ciudadanos;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public SesionEmitida refrescar(String refreshToken) {
        ServicioTokens.TokenValido valido = tokens.validar(refreshToken, JwtTaan.TIPO_REFRESCO)
                .orElseThrow(ServicioSesion::refrescoInvalido);
        if (valido.rol() == Rol.ciudadano) {
            Ciudadano cuenta = ciudadanos.findById(valido.id())
                    .orElseThrow(ServicioSesion::refrescoInvalido);
            return SesionEmitida.paraCiudadano(
                    tokens.emitirSesion(cuenta.getId(), Rol.ciudadano), cuenta.requiereTelefono());
        }
        Usuario usuario = usuarios.findById(valido.id())
                .filter(Usuario::isActivo)
                .orElseThrow(ServicioSesion::refrescoInvalido);
        return SesionEmitida.paraInstitucional(
                tokens.emitirSesion(usuario.getId(), usuario.getRol()),
                usuario.getRol(), usuario.isCredencialTemporal());
    }

    /**
     * Cierre de sesión (RF-031): si llega tokenDispositivo se elimina la suscripción de
     * avisos push de ese dispositivo (RF-034). Los tokens caducan solos; sin tabla de
     * sesiones no hay nada más que invalidar en el servidor.
     */
    @Transactional
    public void cerrar(long id, Rol rol, String tokenDispositivo) {
        if (rol == Rol.ciudadano && tokenDispositivo != null && !tokenDispositivo.isBlank()) {
            jdbc.update("DELETE FROM suscripcion_aviso WHERE ciudadano_id = ? AND token_dispositivo = ?",
                    id, tokenDispositivo);
        }
    }

    private static ReglaNegocioException refrescoInvalido() {
        return new ReglaNegocioException(422, "REFRESCO_INVALIDO", "Vuelve a iniciar sesión.");
    }
}
