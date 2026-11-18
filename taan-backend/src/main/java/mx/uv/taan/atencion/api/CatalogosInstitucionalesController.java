package mx.uv.taan.atencion.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.uv.taan.administracion.aplicacion.CategoriaVista;
import mx.uv.taan.administracion.aplicacion.ServicioCategorias;
import mx.uv.taan.configuracion.aplicacion.AreaVista;
import mx.uv.taan.configuracion.aplicacion.ServicioAreas;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;

/**
 * Catálogos de solo lectura que usa el personal al atender reportes: categorías activas para
 * confirmar o corregir la clasificación (CU-06) y áreas activas de su organización para
 * asignar o reasignar (CU-07). Roles personal_area y coordinacion.
 */
@RestController
@RequestMapping("/institucional")
public class CatalogosInstitucionalesController {

    private final ServicioCategorias categorias;
    private final ServicioAreas areas;

    public CatalogosInstitucionalesController(ServicioCategorias categorias, ServicioAreas areas) {
        this.categorias = categorias;
        this.areas = areas;
    }

    @GetMapping("/categorias")
    public List<CategoriaVista> categorias() {
        return categorias.listarActivas();
    }

    @GetMapping("/areas")
    public List<AreaVista> areas(UsuarioActual usuario) {
        return areas.listarActivas(usuario.organizacionId());
    }
}
