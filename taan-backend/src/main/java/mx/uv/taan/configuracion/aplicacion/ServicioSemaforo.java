package mx.uv.taan.configuracion.aplicacion;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.configuracion.infraestructura.TiempoObjetivoRepository;

/**
 * Semáforo de antigüedad de cada organización (RF-041): la bandeja lo consulta y la coordinación
 * lo configura con /organizacion/semaforo. Solo tienen tiempo objetivo los estados en los que el
 * reporte espera a la organización; un estado sin configurar se muestra como sin_tiempo.
 */
@Service
public class ServicioSemaforo {

    /** Valores de estado_reporte (en texto, como viajan en la API y en la tabla). */
    static final Set<String> ESTADOS_CONFIGURABLES = Set.of("recibido", "validado", "asignado", "en_atencion",
            "reabierto");

    private final TiempoObjetivoRepository tiempos;

    public ServicioSemaforo(TiempoObjetivoRepository tiempos) {
        this.tiempos = tiempos;
    }

    @Transactional(readOnly = true)
    public Map<String, TiempoObjetivo> tiemposDe(long organizacionId) {
        return tiempos.deOrganizacion(organizacionId);
    }

    @Transactional(readOnly = true)
    public List<TiempoObjetivoEstado> consultar(long organizacionId) {
        return tiempos.listar(organizacionId);
    }

    /** PUT: la lista reemplaza toda la configuración; un estado que no venga queda sin semáforo. */
    @Transactional
    public List<TiempoObjetivoEstado> configurar(long organizacionId, List<TiempoObjetivoEstado> nuevos) {
        Set<String> vistos = new HashSet<>();
        for (TiempoObjetivoEstado t : nuevos) {
            // Set.of no admite contains(null): un estado ausente se revisa aparte.
            if (t == null || t.estado() == null || !ESTADOS_CONFIGURABLES.contains(t.estado())) {
                throw invalido("ESTADO_NO_CONFIGURABLE", "Solo se configuran los estados recibido, validado, "
                        + "asignado, en_atencion y reabierto.");
            }
            if (!vistos.add(t.estado())) {
                throw invalido("ESTADO_REPETIDO", "El estado " + t.estado() + " viene más de una vez.");
            }
            if (t.horasAmarillo() == null || t.horasAmarillo() < 1
                    || t.horasRojo() == null || t.horasRojo() <= t.horasAmarillo()) {
                throw invalido("VALIDACION", "En " + t.estado()
                        + ", horasAmarillo debe ser al menos 1 y horasRojo mayor que horasAmarillo.");
            }
        }
        tiempos.reemplazar(organizacionId, nuevos);
        return tiempos.listar(organizacionId);
    }

    private static ReglaNegocioException invalido(String codigo, String mensaje) {
        return new ReglaNegocioException(400, codigo, mensaje);
    }
}
