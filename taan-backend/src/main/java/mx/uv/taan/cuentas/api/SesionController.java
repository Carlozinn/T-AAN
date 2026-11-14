package mx.uv.taan.cuentas.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.comun.seguridad.JwtTaan;
import mx.uv.taan.cuentas.api.dto.CierreSolicitud;
import mx.uv.taan.cuentas.api.dto.RefrescoSolicitud;
import mx.uv.taan.cuentas.aplicacion.ServicioSesion;
import mx.uv.taan.cuentas.aplicacion.SesionEmitida;
import mx.uv.taan.cuentas.dominio.Rol;

/** Renovación (pública, con el refreshToken) y cierre de sesión (CU-04). */
@RestController
public class SesionController {

    private final ServicioSesion sesiones;

    public SesionController(ServicioSesion sesiones) {
        this.sesiones = sesiones;
    }

    @PostMapping("/auth/refresco")
    public SesionEmitida refrescar(@Valid @RequestBody RefrescoSolicitud solicitud) {
        return sesiones.refrescar(solicitud.refreshToken());
    }

    @PostMapping("/auth/cierre")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrar(@RequestBody(required = false) CierreSolicitud solicitud,
                       @AuthenticationPrincipal Jwt sesion) {
        sesiones.cerrar(Long.parseLong(sesion.getSubject()),
                Rol.valueOf(sesion.getClaimAsString(JwtTaan.CLAIM_ROL)),
                solicitud == null ? null : solicitud.tokenDispositivo());
    }
}
