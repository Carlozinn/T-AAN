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
import mx.uv.taan.configuracion.api.dto.AreaActualizacionSolicitud;
import mx.uv.taan.configuracion.api.dto.AreaNuevaSolicitud;
import mx.uv.taan.configuracion.aplicacion.AreaVista;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;

/** Áreas de la organización administradas por su coordinación (CU-13; rol coordinacion). */
@RestController
@RequestMapping("/organizacion/areas")
public class AreasController {

    private final ServicioAreas areas;

    public AreasController(ServicioAreas areas) {
        this.areas = areas;
    }

    @GetMapping
    public List<AreaVista> listar(UsuarioActual coordinacion) {
        return areas.listar(coordinacion.organizacionId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AreaVista crear(@Valid @RequestBody AreaNuevaSolicitud solicitud, UsuarioActual coordinacion) {
        return areas.crear(coordinacion.organizacionId(), solicitud.nombre(),
                solicitud.telefonoPublico(), solicitud.correoPublico());
    }

    @PatchMapping("/{id}")
    public AreaVista actualizar(@PathVariable long id, @Valid @RequestBody AreaActualizacionSolicitud solicitud,
                                UsuarioActual coordinacion) {
        return areas.actualizar(coordinacion.organizacionId(), id, solicitud.nombre(),
                solicitud.telefonoPublico(), solicitud.correoPublico(), solicitud.activa());
    }
}
