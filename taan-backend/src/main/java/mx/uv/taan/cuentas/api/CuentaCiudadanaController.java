package mx.uv.taan.cuentas.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.cuentas.api.dto.EliminacionCuentaSolicitud;
import mx.uv.taan.cuentas.api.dto.PerfilActualizacionSolicitud;
import mx.uv.taan.cuentas.aplicacion.PerfilCiudadano;
import mx.uv.taan.cuentas.aplicacion.ServicioPerfilCiudadano;

/**
 * Perfil y eliminación de la cuenta ciudadana (CU-25). Exige rol ciudadano
 * (ConfiguracionSeguridad: /ciudadano/**). El cambio de contraseña vive en
 * PasswordCiudadanoController.
 */
@RestController
@RequestMapping("/ciudadano")
public class CuentaCiudadanaController {

    private final ServicioPerfilCiudadano perfiles;

    public CuentaCiudadanaController(ServicioPerfilCiudadano perfiles) {
        this.perfiles = perfiles;
    }

    @GetMapping("/perfil")
    public PerfilCiudadano consultar(@AuthenticationPrincipal Jwt sesion) {
        return perfiles.consultar(idDe(sesion));
    }

    @PatchMapping("/perfil")
    public PerfilCiudadano actualizar(@Valid @RequestBody PerfilActualizacionSolicitud solicitud,
                                      @AuthenticationPrincipal Jwt sesion) {
        return perfiles.actualizar(idDe(sesion), solicitud.nombre(), solicitud.telefono(), solicitud.avisosCorreo());
    }

    @PostMapping("/cuenta/eliminacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Valid @RequestBody EliminacionCuentaSolicitud solicitud,
                         @AuthenticationPrincipal Jwt sesion) {
        perfiles.eliminar(idDe(sesion));
    }

    private static long idDe(Jwt sesion) {
        return Long.parseLong(sesion.getSubject());
    }
}
