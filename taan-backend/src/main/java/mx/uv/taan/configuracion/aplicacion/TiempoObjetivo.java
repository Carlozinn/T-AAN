package mx.uv.taan.configuracion.aplicacion;

/** Horas a partir de las cuales un reporte está «por vencer» (amarillo) o «atrasado» (rojo) en un estado. */
public record TiempoObjetivo(int horasAmarillo, int horasRojo) {
}
