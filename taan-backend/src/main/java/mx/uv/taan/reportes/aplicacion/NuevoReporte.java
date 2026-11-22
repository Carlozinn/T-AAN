package mx.uv.taan.reportes.aplicacion;

import mx.uv.taan.reportes.dominio.OrigenReporte;

/** Datos ya validados de un reporte nuevo (DatosNuevoReporte del contrato). */
public record NuevoReporte(String descripcion, double latitud, double longitud, OrigenReporte origen,
                           String campoAuxiliar) {
}
