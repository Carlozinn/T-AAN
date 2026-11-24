package mx.uv.taan.cuentas.aplicacion;

/** A quién se envía un aviso por correo del seguimiento de reportes (RF-044). */
public record DestinatarioAviso(String correo, String nombre) {
}
