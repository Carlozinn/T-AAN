package mx.uv.taan.administracion.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.administracion.api.dto.CoberturaZonaSolicitud;
import mx.uv.taan.administracion.api.dto.ZonaActualizacionSolicitud;
import mx.uv.taan.administracion.api.dto.ZonaNuevaSolicitud;
import mx.uv.taan.administracion.aplicacion.ServicioZonas;
import mx.uv.taan.administracion.aplicacion.ZonaVista;

/** Zonas de cobertura (CU-15; rol administrador). */
@RestController
@RequestMapping("/admin/zonas")
public class ZonasController {

    private final ServicioZonas zonas;

    public ZonasController(ServicioZonas zonas) {
        this.zonas = zonas;
    }

    @GetMapping
    public List<ZonaVista> listar() {
        return zonas.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZonaVista crear(@Valid @RequestBody ZonaNuevaSolicitud solicitud) {
        return zonas.crear(solicitud.nombre(), solicitud.organizacionReceptoraId(), solicitud.geometria());
    }

    @PatchMapping("/{id}")
    public ZonaVista actualizar(@PathVariable long id, @Valid @RequestBody ZonaActualizacionSolicitud solicitud) {
        return zonas.actualizar(id, solicitud.nombre(), solicitud.organizacionReceptoraId(),
                solicitud.geometria(), solicitud.activa());
    }

    @PutMapping("/{id}/organizaciones")
    public ZonaVista definirCobertura(@PathVariable long id, @Valid @RequestBody CoberturaZonaSolicitud solicitud) {
        return zonas.definirCobertura(id, solicitud.organizacionIds());
    }
}
