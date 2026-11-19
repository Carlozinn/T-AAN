package mx.uv.taan.reportes.dominio;

/** Por qué el reporte llegó a su organización (tipo motivo_canalizacion; RF-037). */
public enum MotivoCanalizacion {

    /** Una regla aplicable de mayor prioridad. */
    regla,

    /** Sin regla aplicable: organización receptora por defecto. */
    sin_regla,

    /** Reglas con la misma prioridad máxima: receptora por defecto (CU-16 EX-03). */
    empate_reglas,

    /** La IA no pudo clasificarlo: receptora por defecto (CU-01 EX-04). */
    sin_clasificacion,

    /** Redirigido por el personal institucional (CU-21, incremento 4). */
    redireccion
}
