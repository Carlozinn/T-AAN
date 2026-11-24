package mx.uv.taan.cuentas.aplicacion;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.cuentas.dominio.Ciudadano;
import mx.uv.taan.cuentas.dominio.LevantamientoSuspension;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;
import mx.uv.taan.cuentas.infraestructura.LevantamientoSuspensionRepository;

/**
 * Lo que la moderación (CU-26) necesita de las cuentas ciudadanas: suspenderlas y levantar la
 * suspensión, con aviso al ciudadano, y conocer sus nombres. Las reglas del señalamiento viven en
 * el módulo atencion.
 */
@Service
public class ServicioSuspensiones {

    private static final Logger log = LoggerFactory.getLogger(ServicioSuspensiones.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Mexico_City"));

    private final CiudadanoRepository ciudadanos;
    private final LevantamientoSuspensionRepository levantamientos;
    private final EnviadorCorreo correo;

    public ServicioSuspensiones(CiudadanoRepository ciudadanos, LevantamientoSuspensionRepository levantamientos,
                                EnviadorCorreo correo) {
        this.ciudadanos = ciudadanos;
        this.levantamientos = levantamientos;
        this.correo = correo;
    }

    /**
     * Suspende la cuenta (hasta = null: indefinida) y avisa por correo (RF-043, punto 5).
     * Requiere la transacción de quien llama. false si la cuenta ya no existe.
     */
    @Transactional
    public boolean suspender(long ciudadanoId, String motivo, Instant hasta) {
        return ciudadanos.findById(ciudadanoId).map(cuenta -> {
            cuenta.suspender(motivo, hasta);
            String vigencia = hasta == null ? "por tiempo indefinido" : "hasta el " + FECHA.format(hasta);
            avisar(cuenta, () -> correo.enviarAvisoSuspension(cuenta.getCorreo(), cuenta.getNombre(), motivo, vigencia));
            return true;
        }).orElse(false);
    }

    /**
     * El administrador levanta la suspensión (RF-043): registra quién, cuándo, por qué y qué
     * suspensión se interrumpió, y avisa por correo. Los señalamientos aceptados no se tocan:
     * siguen contando para la reincidencia. Una suspensión ya vencida cuenta como no suspendida (409).
     */
    @Transactional
    public void levantar(long administradorId, long ciudadanoId, String motivo) {
        Ciudadano cuenta = ciudadanos.findById(ciudadanoId).orElseThrow(ReglaNegocioException::noEncontrado);
        if (!cuenta.suspensionVigente(Instant.now())) {
            throw ReglaNegocioException.conflicto("CUENTA_NO_SUSPENDIDA", "Esta cuenta no tiene una suspensión vigente.");
        }
        String texto = motivo.strip();
        levantamientos.save(new LevantamientoSuspension(cuenta, administradorId, texto));
        cuenta.levantarSuspension();
        avisar(cuenta, () -> correo.enviarAvisoLevantamiento(cuenta.getCorreo(), cuenta.getNombre(), texto));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> nombres(Collection<Long> ids) {
        return ciudadanos.findAllById(ids).stream().collect(Collectors.toMap(Ciudadano::getId, Ciudadano::getNombre));
    }

    /** Un fallo del correo nunca deshace la decisión: el ciudadano también la ve en su perfil. */
    private static void avisar(Ciudadano cuenta, Runnable envio) {
        try {
            envio.run();
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el aviso a la cuenta {}", cuenta.getId(), e);
        }
    }
}
