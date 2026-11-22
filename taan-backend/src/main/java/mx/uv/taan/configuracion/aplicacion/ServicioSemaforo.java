package mx.uv.taan.configuracion.aplicacion;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.configuracion.infraestructura.TiempoObjetivoRepository;

/**
 * Semáforo de antigüedad de cada organización (RF-041). En este incremento solo se consulta;
 * la coordinación lo configurará con /organizacion/semaforo (incremento 4B).
 */
@Service
public class ServicioSemaforo {

    private final TiempoObjetivoRepository tiempos;

    public ServicioSemaforo(TiempoObjetivoRepository tiempos) {
        this.tiempos = tiempos;
    }

    @Transactional(readOnly = true)
    public Map<String, TiempoObjetivo> tiemposDe(long organizacionId) {
        return tiempos.deOrganizacion(organizacionId);
    }
}
