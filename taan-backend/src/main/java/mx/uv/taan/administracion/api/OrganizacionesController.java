package mx.uv.taan.administracion.api;

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
import mx.uv.taan.administracion.api.dto.CuentaCoordinacionSolicitud;
import mx.uv.taan.administracion.api.dto.OrganizacionActualizacionSolicitud;
import mx.uv.taan.administracion.api.dto.OrganizacionNuevaSolicitud;
import mx.uv.taan.administracion.aplicacion.OrganizacionVista;
import mx.uv.taan.administracion.aplicacion.ServicioOrganizaciones;
import mx.uv.taan.configuracion.aplicacion.AreaVista;
import mx.uv.taan.cuentas.aplicacion.CredencialTemporal;

/** Organizaciones, su coordinación inicial y la plantilla municipal (CU-11, CU-12, CU-22; rol administrador). */
@RestController
@RequestMapping("/admin/organizaciones")
public class OrganizacionesController {

    private final ServicioOrganizaciones organizaciones;

    public OrganizacionesController(ServicioOrganizaciones organizaciones) {
        this.organizaciones = organizaciones;
    }

    @GetMapping
    public List<OrganizacionVista> listar() {
        return organizaciones.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizacionVista crear(@Valid @RequestBody OrganizacionNuevaSolicitud solicitud) {
        return organizaciones.crear(solicitud.nombre(), solicitud.nivelParticipacion(),
                solicitud.plazoReaperturaDias());
    }

    @PatchMapping("/{id}")
    public OrganizacionVista actualizar(@PathVariable long id,
                                        @Valid @RequestBody OrganizacionActualizacionSolicitud solicitud) {
        return organizaciones.actualizar(id, solicitud.nombre(), solicitud.nivelParticipacion(),
                solicitud.plazoReaperturaDias(), solicitud.activa());
    }

    @PostMapping("/{id}/coordinacion")
    @ResponseStatus(HttpStatus.CREATED)
    public CredencialTemporal crearCoordinacion(@PathVariable long id,
                                                @Valid @RequestBody CuentaCoordinacionSolicitud solicitud) {
        return organizaciones.crearCoordinacion(id, solicitud.nombre(), solicitud.correo());
    }

    @PostMapping("/{id}/plantilla")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AreaVista> aplicarPlantilla(@PathVariable long id) {
        return organizaciones.aplicarPlantilla(id);
    }
}
