package mx.uv.taan.cuentas.aplicacion;

import mx.uv.taan.cuentas.dominio.Rol;

/**
 * Personal institucional que hace la petición, leído de la base en cada solicitud (no del
 * token): así una cuenta desactivada deja de operar de inmediato y un cambio de área se
 * refleja sin volver a iniciar sesión.
 *
 * <p>Los controladores lo reciben como parámetro (UsuarioActualResolver). organizacionId es
 * la segunda barrera de autorización: cada servicio trabaja solo dentro de esa organización.</p>
 */
public record UsuarioActual(long id, Rol rol, Long organizacionId, Long areaId) {
}
