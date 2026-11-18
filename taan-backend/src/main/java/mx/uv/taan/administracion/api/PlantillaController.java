package mx.uv.taan.administracion.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.uv.taan.configuracion.aplicacion.PlantillaAreaVista;
import mx.uv.taan.configuracion.aplicacion.ServicioPlantilla;

/** Consulta de la plantilla municipal (CU-22; rol administrador). */
@RestController
public class PlantillaController {

    private final ServicioPlantilla plantilla;

    public PlantillaController(ServicioPlantilla plantilla) {
        this.plantilla = plantilla;
    }

    @GetMapping("/admin/plantilla")
    public List<PlantillaAreaVista> consultar() {
        return plantilla.consultar();
    }
}
