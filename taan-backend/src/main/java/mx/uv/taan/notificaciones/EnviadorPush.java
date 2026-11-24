package mx.uv.taan.notificaciones;

import java.util.List;
import java.util.Map;

/** Puerto de salida para los avisos push (RF-034). */
public interface EnviadorPush {

    /**
     * Envía el aviso a cada token y devuelve los que el servicio reporta como vencidos o inválidos,
     * para darlos de baja. Los demás errores se registran y no se propagan.
     */
    List<String> enviar(List<String> tokens, String titulo, String cuerpo, Map<String, String> datos);
}
