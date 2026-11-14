package mx.uv.taan.reportes.dominio;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados del reporte y sus transiciones válidas, tal como aparecen en el Anexo G
 * y en el diagrama Estados_TAAN. Es la máquina de estados del backend: ningún servicio
 * cambia el estado de un reporte sin llamar antes a {@link #validarTransicion}.
 *
 * <p>Las condiciones de cada transición (quién la ejecuta, justificación obligatoria, plazo
 * de reapertura, etc.) las verifica el servicio correspondiente; aquí solo se define
 * qué flechas existen.</p>
 */
public enum EstadoReporte {

    PENDIENTE_CANALIZACION("pendiente_canalizacion", "En revisión"),
    RECIBIDO("recibido", "Recibido"),
    VALIDADO("validado", "Validado"),
    ASIGNADO("asignado", "Asignado"),
    EN_ATENCION("en_atencion", "En atención"),
    RESUELTO("resuelto", "Resuelto"),
    REABIERTO("reabierto", "Reabierto"),
    RECHAZADO("rechazado", "Rechazado"),
    DUPLICADO("duplicado", "Duplicado"),
    CANALIZACION_EXTERNA("canalizacion_externa", "Canalización externa");

    private static final Map<EstadoReporte, Set<EstadoReporte>> TRANSICIONES = new EnumMap<>(EstadoReporte.class);

    static {
        // Sistema (reglas): regla aplicable o receptora por defecto Integrada / organización Externa.
        permitir(PENDIENTE_CANALIZACION, RECIBIDO, CANALIZACION_EXTERNA);
        // CU-06 validar (con o sin área), rechazar, duplicado; CU-21 redirigir (otra organización o externa).
        permitir(RECIBIDO, ASIGNADO, VALIDADO, RECHAZADO, DUPLICADO, RECIBIDO, CANALIZACION_EXTERNA);
        // CU-07 asignar a un área.
        permitir(VALIDADO, ASIGNADO);
        // CU-07 reasignar a otra área (el estado no cambia) / CU-08 iniciar atención.
        permitir(ASIGNADO, ASIGNADO, EN_ATENCION);
        // CU-17 registrar resolución.
        permitir(EN_ATENCION, RESUELTO);
        // CU-23 reabrir (una sola vez y dentro del plazo: lo verifica el servicio).
        permitir(RESUELTO, REABIERTO);
        // CU-08 volver a atender un reporte reabierto.
        permitir(REABIERTO, EN_ATENCION);
        // RECHAZADO, DUPLICADO y CANALIZACION_EXTERNA son finales: no tienen salidas.
    }

    private final String valorBd;
    private final String textoCiudadano;

    EstadoReporte(String valorBd, String textoCiudadano) {
        this.valorBd = valorBd;
        this.textoCiudadano = textoCiudadano;
    }

    private static void permitir(EstadoReporte origen, EstadoReporte... destinos) {
        Set<EstadoReporte> conjunto = EnumSet.noneOf(EstadoReporte.class);
        Collections.addAll(conjunto, destinos);
        TRANSICIONES.put(origen, conjunto);
    }

    /** Valor del tipo enumerado estado_reporte en PostgreSQL y en el contrato de la API. */
    public String valorBd() {
        return valorBd;
    }

    /** Texto que se muestra al ciudadano (Pendiente de canalización se muestra como «En revisión»). */
    public String textoCiudadano() {
        return textoCiudadano;
    }

    public boolean puedePasarA(EstadoReporte destino) {
        return TRANSICIONES.getOrDefault(this, Set.of()).contains(destino);
    }

    public boolean esFinal() {
        return TRANSICIONES.getOrDefault(this, Set.of()).isEmpty();
    }

    /** Lanza {@link TransicionInvalidaException} (409 TRANSICION_INVALIDA) si la flecha no existe. */
    public void validarTransicion(EstadoReporte destino) {
        if (!puedePasarA(destino)) {
            throw new TransicionInvalidaException(this, destino);
        }
    }

    public static EstadoReporte desdeValorBd(String valor) {
        for (EstadoReporte e : values()) {
            if (e.valorBd.equals(valor)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado desconocido: " + valor);
    }
}
