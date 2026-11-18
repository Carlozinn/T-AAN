package mx.uv.taan.administracion.dominio;

/**
 * Cómo participa una organización (tipo nivel_participacion de PostgreSQL). Integrada atiende
 * en T'AAN; externa solo recibe canalización con sus canales oficiales (RF-036).
 */
public enum NivelParticipacion {

    integrada,

    /** Iteración posterior al MVP. */
    conectada,

    externa
}
