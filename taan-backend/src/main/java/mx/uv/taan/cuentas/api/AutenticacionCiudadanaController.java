package mx.uv.taan.cuentas.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import mx.uv.taan.cuentas.api.dto.LoginCorreoSolicitud;
import mx.uv.taan.cuentas.api.dto.LoginGoogleSolicitud;
import mx.uv.taan.cuentas.api.dto.RegistroCorreoSolicitud;
import mx.uv.taan.cuentas.api.dto.RestablecimientoSolicitud;
import mx.uv.taan.cuentas.api.dto.SolicitudCorreoSolicitud;
import mx.uv.taan.cuentas.api.dto.TokenVerificacionSolicitud;
import mx.uv.taan.cuentas.aplicacion.ServicioAccesoCiudadano;
import mx.uv.taan.cuentas.aplicacion.ServicioRecuperacionPassword;
import mx.uv.taan.cuentas.aplicacion.ServicioRegistroCiudadano;
import mx.uv.taan.cuentas.aplicacion.SesionEmitida;

/**
 * Autenticación ciudadana del contrato (CU-04, CU-24): registro con correo, verificación,
 * Google e inicio de sesión. Todas las operaciones son públicas (ConfiguracionSeguridad).
 */
@RestController
@RequestMapping("/auth/ciudadanos")
public class AutenticacionCiudadanaController {

    private final ServicioRegistroCiudadano registro;
    private final ServicioAccesoCiudadano acceso;
    private final ServicioRecuperacionPassword recuperacion;

    public AutenticacionCiudadanaController(ServicioRegistroCiudadano registro, ServicioAccesoCiudadano acceso,
                                            ServicioRecuperacionPassword recuperacion) {
        this.registro = registro;
        this.acceso = acceso;
        this.recuperacion = recuperacion;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void registrar(@Valid @RequestBody RegistroCorreoSolicitud solicitud) {
        registro.registrar(solicitud.nombre(), solicitud.correo(), solicitud.password(),
                solicitud.captchaToken(), solicitud.campoAuxiliar());
    }

    @PostMapping("/verificacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificar(@Valid @RequestBody TokenVerificacionSolicitud solicitud) {
        registro.verificar(solicitud.token());
    }

    @PostMapping("/verificacion/reenvio")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reenviar(@Valid @RequestBody SolicitudCorreoSolicitud solicitud) {
        registro.reenviar(solicitud.correo());
    }

    @PostMapping("/recuperacion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void solicitarRecuperacion(@Valid @RequestBody SolicitudCorreoSolicitud solicitud) {
        recuperacion.solicitar(solicitud.correo());
    }

    @PostMapping("/recuperacion/confirmacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecerPassword(@Valid @RequestBody RestablecimientoSolicitud solicitud) {
        recuperacion.restablecer(solicitud.token(), solicitud.nuevaPassword());
    }

    @PostMapping("/sesion")
    public SesionEmitida iniciarSesion(@Valid @RequestBody LoginCorreoSolicitud solicitud,
                                       HttpServletRequest peticion) {
        return acceso.iniciarSesion(solicitud.correo(), solicitud.password(), peticion.getRemoteAddr());
    }

    @PostMapping("/google")
    public SesionEmitida accederConGoogle(@Valid @RequestBody LoginGoogleSolicitud solicitud) {
        return acceso.accederConGoogle(solicitud.idToken(), solicitud.captchaToken(),
                solicitud.aceptaAvisoPrivacidad());
    }
}
