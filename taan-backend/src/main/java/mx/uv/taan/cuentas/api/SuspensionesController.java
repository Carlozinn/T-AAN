package mx.uv.taan.cuentas.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.cuentas.api.dto.LevantamientoSolicitud;
import mx.uv.taan.cuentas.aplicacion.ServicioSuspensiones;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;

/** Levantamiento de suspensiones de cuentas ciudadanas (RF-043; rol administrador). */
@RestController
@RequestMapping("/admin/ciudadanos")
public class SuspensionesController {

    private final ServicioSuspensiones suspensiones;

    public SuspensionesController(ServicioSuspensiones suspensiones) {
        this.suspensiones = suspensiones;
    }

    @PostMapping("/{id}/suspension/levantamiento")
    @ResponseStatus(HttpStatus.CREATED)
    public void levantar(@PathVariable long id, @Valid @RequestBody LevantamientoSolicitud s, UsuarioActual usuario) {
        suspensiones.levantar(usuario.id(), id, s.motivo());
    }
}
