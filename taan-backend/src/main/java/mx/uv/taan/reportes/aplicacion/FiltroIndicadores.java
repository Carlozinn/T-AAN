package mx.uv.taan.reportes.aplicacion;

import java.time.LocalDate;

/**
 * Ámbito y periodo de las estadísticas (CU-10). Lo arma el servidor según quién consulta.
 *
 * @param areaId null para toda la organización
 * @param desde  primer día del periodo (reportes creados desde ese día)
 * @param hasta  último día del periodo, incluido
 */
public record FiltroIndicadores(long organizacionId, Long areaId, LocalDate desde, LocalDate hasta) {
}
