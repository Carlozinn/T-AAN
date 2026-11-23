package mx.uv.taan.atencion.dominio;

import java.time.Duration;
import java.time.Instant;

import mx.uv.taan.configuracion.aplicacion.TiempoObjetivo;

/**
 * Semáforo de antigüedad (RF-041): cuánto lleva un reporte en su estado actual comparado con
 * el tiempo objetivo de su organización para ese estado. Es informativo: no cambia estados.
 * En minúsculas porque así viaja en el contrato (esquema Semaforo).
 */
public enum Semaforo {

    /** En tiempo. */
    verde,

    /** Por vencer: pasó las horas de amarillo. */
    amarillo,

    /** Atrasado: pasó las horas de rojo. */
    rojo,

    /** El estado no tiene tiempo objetivo configurado (p. ej. los estados finales). */
    sin_tiempo;

    public static Semaforo calcular(TiempoObjetivo tiempo, Instant desde, Instant ahora) {
        if (tiempo == null) {
            return sin_tiempo;
        }
        Duration transcurrido = Duration.between(desde, ahora);
        if (transcurrido.compareTo(Duration.ofHours(tiempo.horasRojo())) >= 0) {
            return rojo;
        }
        if (transcurrido.compareTo(Duration.ofHours(tiempo.horasAmarillo())) >= 0) {
            return amarillo;
        }
        return verde;
    }

    /** null si el texto no es un valor válido. */
    public static Semaforo desdeTexto(String texto) {
        for (Semaforo s : values()) {
            if (s.name().equals(texto)) {
                return s;
            }
        }
        return null;
    }
}
