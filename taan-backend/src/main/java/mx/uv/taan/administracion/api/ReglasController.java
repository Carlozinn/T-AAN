package mx.uv.taan.administracion.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.uv.taan.administracion.api.dto.ReglaActualizacionSolicitud;
import mx.uv.taan.administracion.api.dto.ReglaNuevaSolicitud;
import mx.uv.taan.administracion.aplicacion.ReglaVista;
import mx.uv.taan.administracion.aplicacion.ServicioReglas;

/** Reglas de canalización (CU-16; rol administrador). */
@RestController
@RequestMapping("/admin/reglas")
public class ReglasController {

    private final ServicioReglas reglas;

    public ReglasController(ServicioReglas reglas) {
        this.reglas = reglas;
    }

    @GetMapping
    public List<ReglaVista> listar(@RequestParam(required = false) Long zonaId) {
        return reglas.listar(zonaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReglaVista crear(@Valid @RequestBody ReglaNuevaSolicitud s) {
        return reglas.crear(s.zonaId(), s.categoriaId(), s.organizacionId(), s.areaId(),
                s.prioridad(), s.vigenteDesde(), s.vigenteHasta());
    }

    @PatchMapping("/{id}")
    public ReglaVista actualizar(@PathVariable long id, @Valid @RequestBody ReglaActualizacionSolicitud s) {
        return reglas.actualizar(id, s.prioridad(), s.areaId(), s.vigenteHasta(), s.activa());
    }
}
