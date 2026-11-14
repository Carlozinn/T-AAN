package mx.uv.taan.comun.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita la ejecución en segundo plano: el reporte responde de inmediato con el folio
 * y la IA y las reglas trabajan después (RNF-011).
 */
@Configuration
@EnableAsync
public class ConfiguracionAsincrona {
}
