package mx.uv.taan.reportes.aplicacion;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import mx.uv.taan.ia.ClasificadorIa;
import mx.uv.taan.ia.ResultadoClasificacion;

/**
 * Procesamiento asíncrono de los reportes (CU-01 paso 6, RNF-011): clasificar con IA y
 * canalizar con reglas, sin que el ciudadano espere.
 *
 * <ul>
 *   <li>Arranca en cuanto se confirma el registro del reporte (evento ReporteRegistrado).</li>
 *   <li>Cada cierto tiempo recoge los pendientes que quedaron sin procesar, por ejemplo si el
 *       servidor se reinició: ningún reporte se queda «En revisión» para siempre.</li>
 * </ul>
 * Si la IA falla (ambos proveedores), el reporte se canaliza igual, sin clasificación, a la
 * organización receptora por defecto (CU-01 EX-04).
 */
@Component
public class ProcesadorReportes {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorReportes.class);

    private final ServicioCanalizacion canalizacion;
    private final ClasificadorIa clasificador;

    public ProcesadorReportes(ServicioCanalizacion canalizacion, ClasificadorIa clasificador) {
        this.canalizacion = canalizacion;
        this.clasificador = clasificador;
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

    void procesar(long reporteId) {
        try {
            Optional<String> descripcion = canalizacion.descripcionSiPendiente(reporteId);
            if (descripcion.isEmpty()) {
                return;
            }
            canalizacion.aplicar(reporteId, clasificar(descripcion.get()));
        } catch (RuntimeException e) {
            // Queda pendiente y la tarea programada lo reintentará.
            log.error("No se pudo canalizar el reporte {}; se reintentará.", reporteId, e);
        }
    }

    private ResultadoClasificacion clasificar(String descripcion) {
        try {
            return clasificador.clasificar(descripcion, canalizacion.catalogoParaIa());
        } catch (RuntimeException e) {
            log.warn("La IA no pudo clasificar: {}", e.getMessage());
            return ResultadoClasificacion.sinClasificacion();
        }
    }
}
