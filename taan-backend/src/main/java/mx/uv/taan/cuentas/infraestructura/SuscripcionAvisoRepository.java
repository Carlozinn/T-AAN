package mx.uv.taan.cuentas.infraestructura;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import mx.uv.taan.cuentas.dominio.SuscripcionAviso;

/** Tokens de dispositivo para avisos push. Solo la usan los servicios del módulo cuentas. */
public interface SuscripcionAvisoRepository extends JpaRepository<SuscripcionAviso, Long> {

    /** Alta idempotente: si el dispositivo ya estaba registrado en la cuenta no hace nada. */
    @Modifying
    @Query(value = """
            INSERT INTO suscripcion_aviso (ciudadano_id, token_dispositivo) VALUES (:ciudadanoId, :token)
            ON CONFLICT (ciudadano_id, token_dispositivo) DO NOTHING""", nativeQuery = true)
    void registrar(long ciudadanoId, String token);

    /** Quita el dispositivo de las demás cuentas: los avisos de otra persona no deben llegar a este teléfono. */
    @Modifying
    @Query("DELETE FROM SuscripcionAviso s WHERE s.tokenDispositivo = :token AND s.ciudadanoId <> :ciudadanoId")
    void eliminarDeOtrasCuentas(long ciudadanoId, String token);

    @Modifying
    @Query("DELETE FROM SuscripcionAviso s WHERE s.ciudadanoId = :ciudadanoId AND s.tokenDispositivo IN :tokens")
    void eliminar(long ciudadanoId, Collection<String> tokens);

    @Query("SELECT s.tokenDispositivo FROM SuscripcionAviso s WHERE s.ciudadanoId = :ciudadanoId")
    List<String> tokensDe(long ciudadanoId);
}
