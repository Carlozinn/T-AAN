package mx.uv.taan.configuracion.aplicacion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.configuracion.infraestructura.PlantillaRepository;

/** Consulta de la plantilla municipal (CU-22, RF-039). */
@Service
public class ServicioPlantilla {

    private final PlantillaRepository plantilla;

    public ServicioPlantilla(PlantillaRepository plantilla) {
        this.plantilla = plantilla;
    }

    @Transactional(readOnly = true)
    public List<PlantillaAreaVista> consultar() {
        return plantilla.consultar();
    }
}
