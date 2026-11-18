package mx.uv.taan.reportes.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.uv.taan.administracion.aplicacion.CoberturaVista;
import mx.uv.taan.administracion.aplicacion.ServicioZonas;

/**
 * Verificación de cobertura antes de enviar un reporte (CU-01 EX-05, RF-014; rol ciudadano).
 * La app la consulta al fijar el punto en el mapa para avisar si la zona aún no tiene servicio.
 */
@RestController
public class CoberturaController {

    private final ServicioZonas zonas;

    public CoberturaController(ServicioZonas zonas) {
        this.zonas = zonas;
    }

    @GetMapping("/cobertura")
    public CoberturaVista verificar(@RequestParam double latitud, @RequestParam double longitud) {
        return zonas.cobertura(latitud, longitud);
    }
}
