package mx.uv.taan.administracion.aplicacion;

/**
 * Esquema Correspondencia del contrato: qué área atiende cada categoría en la organización.
 * origen es «plantilla» si el área coincide con la de la plantilla municipal, «coordinacion»
 * en cualquier otro caso; «ia» se usará con la propuesta asistida (incremento 5).
 */
public record CorrespondenciaVista(long categoriaId, String categoria, Long areaId, String area, String origen) {
}
