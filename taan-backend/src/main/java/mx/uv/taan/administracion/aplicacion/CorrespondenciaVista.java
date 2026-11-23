package mx.uv.taan.administracion.aplicacion;

/**
 * Esquema Correspondencia del contrato: qué área atiende cada categoría en la organización.
 * origen es «ia» si lo propone la IA (aún sin confirmar), «plantilla» si el área coincide con la
 * de la plantilla municipal y «coordinacion» en cualquier otro caso.
 */
public record CorrespondenciaVista(long categoriaId, String categoria, Long areaId, String area, String origen) {
}
