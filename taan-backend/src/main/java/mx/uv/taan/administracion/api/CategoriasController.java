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
import mx.uv.taan.administracion.api.dto.CategoriaActualizacionSolicitud;
import mx.uv.taan.administracion.api.dto.CategoriaNuevaSolicitud;
import mx.uv.taan.administracion.aplicacion.CategoriaVista;
import mx.uv.taan.administracion.aplicacion.ServicioCategorias;

/** Catálogo común de categorías (CU-14; rol administrador). */
@RestController
@RequestMapping("/admin/categorias")
public class CategoriasController {

    private final ServicioCategorias categorias;

    public CategoriasController(ServicioCategorias categorias) {
        this.categorias = categorias;
    }

    @GetMapping
    public List<CategoriaVista> listar() {
        return categorias.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaVista crear(@Valid @RequestBody CategoriaNuevaSolicitud solicitud) {
        return categorias.crear(solicitud.nombre(), solicitud.categoriaPadreId());
    }

    @PatchMapping("/{id}")
    public CategoriaVista actualizar(@PathVariable long id, @Valid @RequestBody CategoriaActualizacionSolicitud solicitud) {
        return categorias.actualizar(id, solicitud.nombre(), solicitud.activa());
    }
}
