package mx.uv.taan.cuentas.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.cuentas.api.dto.CambioPasswordSolicitud;
import mx.uv.taan.cuentas.aplicacion.ServicioAccesoCiudadano;

/**
 * Cambio de contraseña de la cuenta ciudadana (CU-25, RF-012). Solo cuentas de correo;
 * las de Google responden 422 (FA-01). El resto del perfil llega en el incremento de
 * cuenta ciudadana.
 */
@RestController
public class PasswordCiudadanoController {

    private final ServicioAccesoCiudadano acceso;

    public PasswordCiudadanoController(ServicioAccesoCiudadano acceso) {
        this.acceso = acceso;
    }

    @PutMapping("/ciudadano/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@Valid @RequestBody CambioPasswordSolicitud solicitud,
                                @AuthenticationPrincipal Jwt sesion) {
        acceso.cambiarPassword(Long.parseLong(sesion.getSubject()),
                solicitud.passwordActual(), solicitud.nuevaPassword());
    }
}
