package mx.uv.taan.cuentas.aplicacion;

import java.time.Instant;

/**
 * Lo que el módulo de reportes necesita saber de una cuenta antes de aceptar un reporte
 * (CU-01 EX-07 y EX-08): sin datos personales más allá de la suspensión.
 *
 * @param suspendida true solo si la suspensión sigue vigente (indefinida o con fecha futura)
 */
public record CondicionCiudadano(
        boolean correoVerificado,
        boolean requiereTelefono,
        boolean suspendida,
        String motivoSuspension,
        Instant suspendidaHasta) {
}
