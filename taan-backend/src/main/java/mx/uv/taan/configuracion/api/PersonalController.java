package mx.uv.taan.configuracion.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.configuracion.api.dto.UsuarioActualizacionSolicitud;
import mx.uv.taan.configuracion.api.dto.UsuarioNuevoSolicitud;
import mx.uv.taan.configuracion.aplicacion.ServicioPersonal;
import mx.uv.taan.cuentas.aplicacion.CredencialTemporal;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;
import mx.uv.taan.cuentas.aplicacion.UsuarioInstitucional;

/** Personal de la organización administrado por su coordinación (CU-12; rol coordinacion). */
@RestController
@RequestMapping("/organizacion/usuarios")
public class PersonalController {

    private final ServicioPersonal personal;

    public PersonalController(ServicioPersonal personal) {
        this.personal = personal;
    }

    @GetMapping
    public List<UsuarioInstitucional> listar(UsuarioActual coordinacion) {
        return personal.listar(coordinacion);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredencialTemporal crear(@Valid @RequestBody UsuarioNuevoSolicitud solicitud, UsuarioActual coordinacion) {
        return personal.crear(coordinacion, solicitud.nombre(), solicitud.correo(), solicitud.rol(), solicitud.areaId());
    }

    @PatchMapping("/{id}")
    public UsuarioInstitucional actualizar(@PathVariable long id,
                                           @Valid @RequestBody UsuarioActualizacionSolicitud solicitud,
                                           UsuarioActual coordinacion) {
        return personal.actualizar(coordinacion, id, solicitud.nombre(), solicitud.rol(),
                solicitud.areaId(), solicitud.activo());
    }

    @PostMapping("/{id}/credencial-temporal")
    public CredencialTemporal restablecerCredencial(@PathVariable long id, UsuarioActual coordinacion) {
        return personal.restablecerCredencial(coordinacion, id);
    }
}
