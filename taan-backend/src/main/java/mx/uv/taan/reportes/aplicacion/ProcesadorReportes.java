package mx.uv.taan.reportes.aplicacion;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import mx.uv.taan.ia.ClasificadorIa;
import mx.uv.taan.ia.IaNoDisponibleException;
import mx.uv.taan.ia.ResultadoClasificacion;

/**
 * Procesamiento asíncrono de los reportes (CU-01 paso 6, RNF-011): clasificar con IA y
 * canalizar con reglas, sin que el ciudadano espere.
 *
 * <ul>
 *   <li>Arranca en cuanto se confirma el registro del reporte (evento ReporteRegistrado).</li>
 *   <li>Cada cierto tiempo recoge los pendientes que quedaron sin procesar: los que la IA no pudo
 *       clasificar y los que se quedaron a medias, por ejemplo si el servidor se reinició.</li>
 * </ul>
 * Si fallan los dos proveedores, el reporte sigue pendiente y se reintenta hasta
 * taan.ia.intentos-maximos veces; después se canaliza sin clasificación a la organización
 * receptora por defecto (RF-007 punto 5, CU-01 EX-04).
 */
@Component
public class ProcesadorReportes {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorReportes.class);

    private final ServicioCanalizacion canalizacion;
    private final ClasificadorIa clasificador;
    private final int intentosMaximos;

    public ProcesadorReportes(ServicioCanalizacion canalizacion, ClasificadorIa clasificador,
                              @Value("${taan.ia.intentos-maximos}") int intentosMaximos) {
        this.canalizacion = canalizacion;
        this.clasificador = clasificador;
        this.intentosMaximos = intentosMaximos;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrar(ReporteRegistrado evento) {
        procesar(evento.reporteId());
    }

    @Scheduled(initialDelay = 60_000, fixedDelayString = "${taan.reportes.reintento-ms:300000}")
    public void reintentarPendientes() {
        canalizacion.pendientesParaReintentar().forEach(this::procesar);
    }

    /** La IA se llama fuera de toda transacción: ninguna fila queda bloqueada mientras responde. */
    void procesar(long reporteId) {
        try {
            Optional<String> descripcion = canalizacion.descripcionSiPendiente(reporteId);
            if (descripcion.isEmpty()) {
                return;
            }
            ResultadoClasificacion resultado;
            try {
                resultado = clasificador.clasificar(descripcion.get(), canalizacion.catalogoParaIa());
            } catch (IaNoDisponibleException e) {
                if (!canalizacion.registrarFalloIa(reporteId, intentosMaximos)) {
                    log.warn("La IA no clasificó el reporte {}; se reintentará.", reporteId);
                    return;
                }
                log.warn("La IA no clasificó el reporte {} tras {} intentos; se canaliza sin clasificación.",
                        reporteId, intentosMaximos);
                resultado = ResultadoClasificacion.sinClasificacion(ResultadoClasificacion.IA_NO_DISPONIBLE);
            }
            canalizacion.aplicar(reporteId, resultado);
        } catch (RuntimeException e) {
            // Queda pendiente y la tarea programada lo reintentará.
            log.error("No se pudo canalizar el reporte {}; se reintentará.", reporteId, e);
        }
    }
}
