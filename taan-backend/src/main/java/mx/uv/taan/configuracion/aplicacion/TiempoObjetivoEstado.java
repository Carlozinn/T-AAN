package mx.uv.taan.configuracion.aplicacion;

/**
 * Esquema TiempoObjetivo del contrato (entrada y salida de /organizacion/semaforo). Los campos
 * admiten null para que ServicioSemaforo responda 400 con un mensaje claro si faltan.
 */
public record TiempoObjetivoEstado(String estado, Integer horasAmarillo, Integer horasRojo) {
}
