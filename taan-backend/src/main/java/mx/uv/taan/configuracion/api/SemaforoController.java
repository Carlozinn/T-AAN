package mx.uv.taan.configuracion.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.uv.taan.configuracion.aplicacion.ServicioSemaforo;
import mx.uv.taan.configuracion.aplicacion.TiempoObjetivoEstado;
import mx.uv.taan.cuentas.aplicacion.UsuarioActual;

/** Tiempos objetivo del semáforo de la propia organización (RF-041; rol coordinacion). */
@RestController
@RequestMapping("/organizacion/semaforo")
public class SemaforoController {

    private final ServicioSemaforo semaforo;

    public SemaforoController(ServicioSemaforo semaforo) {
        this.semaforo = semaforo;
    }

    @GetMapping
    public List<TiempoObjetivoEstado> consultar(UsuarioActual coordinacion) {
        return semaforo.consultar(coordinacion.organizacionId());
    }

    /** Las reglas de cada elemento las valida el servicio (400 con código y mensaje). */
    @PutMapping
    public List<TiempoObjetivoEstado> configurar(@RequestBody List<TiempoObjetivoEstado> tiempos,
                                                 UsuarioActual coordinacion) {
        return semaforo.configurar(coordinacion.organizacionId(), tiempos);
    }
}
