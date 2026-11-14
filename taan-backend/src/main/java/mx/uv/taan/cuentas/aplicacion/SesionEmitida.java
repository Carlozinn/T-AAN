package mx.uv.taan.cuentas.aplicacion;

import com.fasterxml.jackson.annotation.JsonInclude;

import mx.uv.taan.cuentas.dominio.Rol;
import mx.uv.taan.cuentas.infraestructura.ServicioTokens.ParDeTokens;

/**
 * Esquema Sesion del contrato (TAAN_API.yaml). requiereTelefono solo aplica a ciudadanos
 * y requiereCambioPassword solo a institucionales; el campo que no aplica no se serializa.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SesionEmitida(
        String accessToken,
        String refreshToken,
        long expiraEn,
        Rol rol,
        Boolean requiereTelefono,
        Boolean requiereCambioPassword) {

    public static SesionEmitida paraCiudadano(ParDeTokens tokens, boolean requiereTelefono) {
        return new SesionEmitida(tokens.accessToken(), tokens.refreshToken(), tokens.expiraEn(),
                Rol.ciudadano, requiereTelefono, null);
    }

    public static SesionEmitida paraInstitucional(ParDeTokens tokens, Rol rol, boolean requiereCambioPassword) {
        return new SesionEmitida(tokens.accessToken(), tokens.refreshToken(), tokens.expiraEn(),
                rol, null, requiereCambioPassword);
    }
}
