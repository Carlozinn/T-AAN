package mx.uv.taan.cuentas.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.cuentas.api.dto.DispositivoSolicitud;
import mx.uv.taan.cuentas.aplicacion.ServicioDispositivos;

/** Alta (CU-04) y baja (CU-25) del dispositivo para avisos push (RF-034). Exige rol ciudadano. */
@RestController
@RequestMapping("/ciudadano/dispositivos")
public class DispositivosController {

    private final ServicioDispositivos dispositivos;

    public DispositivosController(ServicioDispositivos dispositivos) {
        this.dispositivos = dispositivos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrar(@Valid @RequestBody DispositivoSolicitud solicitud, @AuthenticationPrincipal Jwt sesion) {
        dispositivos.registrar(Long.parseLong(sesion.getSubject()), solicitud.tokenDispositivo());
    }

    @PostMapping("/baja")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@Valid @RequestBody DispositivoSolicitud solicitud, @AuthenticationPrincipal Jwt sesion) {
        dispositivos.darDeBaja(Long.parseLong(sesion.getSubject()), solicitud.tokenDispositivo());
    }
}
