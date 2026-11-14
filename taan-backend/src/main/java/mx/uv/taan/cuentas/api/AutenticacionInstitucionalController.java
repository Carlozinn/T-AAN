package mx.uv.taan.cuentas.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import mx.uv.taan.cuentas.api.dto.CambioPasswordSolicitud;
import mx.uv.taan.cuentas.api.dto.LoginCorreoSolicitud;
import mx.uv.taan.cuentas.aplicacion.ServicioAccesoInstitucional;
import mx.uv.taan.cuentas.aplicacion.SesionEmitida;

/**
 * Autenticación institucional del contrato (CU-04): inicio de sesión del personal y cambio
 * de contraseña. El cambio exige sesión con rol institucional (ConfiguracionSeguridad).
 */
@RestController
@RequestMapping("/auth/institucional")
public class AutenticacionInstitucionalController {

    private final ServicioAccesoInstitucional acceso;

    public AutenticacionInstitucionalController(ServicioAccesoInstitucional acceso) {
        this.acceso = acceso;
    }

    @PostMapping("/sesion")
    public SesionEmitida iniciarSesion(@Valid @RequestBody LoginCorreoSolicitud solicitud,
                                       HttpServletRequest peticion) {
        return acceso.iniciarSesion(solicitud.correo(), solicitud.password(), peticion.getRemoteAddr());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@Valid @RequestBody CambioPasswordSolicitud solicitud,
                                @AuthenticationPrincipal Jwt sesion) {
        acceso.cambiarPassword(Long.parseLong(sesion.getSubject()),
                solicitud.passwordActual(), solicitud.nuevaPassword());
    }
}
