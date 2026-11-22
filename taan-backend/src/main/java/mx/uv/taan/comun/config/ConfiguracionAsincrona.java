package mx.uv.taan.comun.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita la ejecución en segundo plano: el reporte responde de inmediato con el folio
 * y la IA y las reglas trabajan después (RNF-011). Las tareas programadas recogen los
 * reportes que quedaron pendientes (por ejemplo, si el servidor se reinició a mitad del
 * procesamiento), para que ninguno se pierda.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class ConfiguracionAsincrona {
}
