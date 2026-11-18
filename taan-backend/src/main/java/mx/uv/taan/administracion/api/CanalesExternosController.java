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
import mx.uv.taan.administracion.api.dto.CanalExternoActualizacionSolicitud;
import mx.uv.taan.administracion.api.dto.CanalExternoNuevoSolicitud;
import mx.uv.taan.administracion.aplicacion.CanalExternoVista;
import mx.uv.taan.administracion.aplicacion.ServicioCanalesExternos;

/** Canales oficiales de organizaciones externas (CU-20; rol administrador). */
@RestController
@RequestMapping("/admin/canales-externos")
public class CanalesExternosController {

    private final ServicioCanalesExternos canales;

    public CanalesExternosController(ServicioCanalesExternos canales) {
        this.canales = canales;
    }

    @GetMapping
    public List<CanalExternoVista> listar() {
        return canales.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CanalExternoVista crear(@Valid @RequestBody CanalExternoNuevoSolicitud s) {
        return canales.crear(s.organizacionId(), s.tipo(), s.valor(), s.fuenteVerificacion());
    }

    @PatchMapping("/{id}")
    public CanalExternoVista actualizar(@PathVariable long id, @Valid @RequestBody CanalExternoActualizacionSolicitud s) {
        return canales.actualizar(id, s.tipo(), s.valor(), s.fuenteVerificacion(), s.activo());
    }
}
