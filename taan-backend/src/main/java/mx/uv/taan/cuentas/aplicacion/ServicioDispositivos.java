package mx.uv.taan.cuentas.aplicacion;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.cuentas.infraestructura.SuscripcionAvisoRepository;

/**
 * Dispositivos de la cuenta ciudadana que reciben avisos push (RF-034). Un mismo dispositivo
 * pertenece a una sola cuenta a la vez: si otra persona inicia sesión en él, deja de recibir
 * los avisos de la cuenta anterior. Al eliminar la cuenta, la base borra sus tokens en cascada.
 */
@Service
public class ServicioDispositivos {

    private final SuscripcionAvisoRepository suscripciones;

    public ServicioDispositivos(SuscripcionAvisoRepository suscripciones) {
        this.suscripciones = suscripciones;
    }

    @Transactional
    public void registrar(long ciudadanoId, String token) {
        String limpio = token.strip();
        suscripciones.eliminarDeOtrasCuentas(ciudadanoId, limpio);
        suscripciones.registrar(ciudadanoId, limpio);
    }

    /** Baja del dispositivo (también al cerrar sesión en él). Si no estaba registrado, no pasa nada. */
    @Transactional
    public void darDeBaja(long ciudadanoId, String token) {
        eliminar(ciudadanoId, Set.of(token.strip()));
    }

    @Transactional(readOnly = true)
    public List<String> tokensDe(long ciudadanoId) {
        return suscripciones.tokensDe(ciudadanoId);
    }

    /** Elimina los tokens que Firebase reporta como vencidos o inválidos. */
    @Transactional
    public void eliminar(long ciudadanoId, Collection<String> tokens) {
        if (!tokens.isEmpty()) {
            suscripciones.eliminar(ciudadanoId, tokens);
        }
    }
}
