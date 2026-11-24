package mx.uv.taan.cuentas.dominio;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Token de Firebase Cloud Messaging de un dispositivo de la cuenta (tabla suscripcion_aviso; RF-034).
 * Las altas y bajas se hacen con consultas directas (SuscripcionAvisoRepository); la entidad solo mapea la tabla.
 */
@Entity
@Table(name = "suscripcion_aviso")
public class SuscripcionAviso {

    @Id
    private Long id;

    @Column(name = "ciudadano_id", nullable = false)
    private Long ciudadanoId;

    @Column(name = "token_dispositivo", nullable = false)
    private String tokenDispositivo;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected SuscripcionAviso() {
        // Requerido por JPA.
    }
}
