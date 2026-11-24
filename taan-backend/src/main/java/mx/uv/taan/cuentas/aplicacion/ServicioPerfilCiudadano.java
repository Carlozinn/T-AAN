package mx.uv.taan.cuentas.aplicacion;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.comun.error.ReglaNegocioException;
import mx.uv.taan.comun.seguridad.CifradorDatos;
import mx.uv.taan.cuentas.dominio.Ciudadano;
import mx.uv.taan.cuentas.dominio.EstadoCuenta;
import mx.uv.taan.cuentas.infraestructura.CiudadanoRepository;

/**
 * Perfil y eliminación de la cuenta ciudadana (CU-25; RF-012, RF-038, RF-044).
 *
 * <p>El teléfono se cifra al guardarlo y se descifra solo para mostrárselo a su dueño.
 * Una cuenta suspendida también puede consultar y corregir su perfil.</p>
 */
@Service
public class ServicioPerfilCiudadano {

    private final CiudadanoRepository ciudadanos;
    private final CifradorDatos cifrador;

    public ServicioPerfilCiudadano(CiudadanoRepository ciudadanos, CifradorDatos cifrador) {
        this.ciudadanos = ciudadanos;
        this.cifrador = cifrador;
    }

    @Transactional(readOnly = true)
    public PerfilCiudadano consultar(long ciudadanoId) {
        return perfilDe(buscar(ciudadanoId));
    }

    /** Condición de la cuenta para crear reportes (CU-01). Una suspensión con fecha ya vencida no cuenta. */
    @Transactional(readOnly = true)
    public CondicionCiudadano condicionParaReportar(long ciudadanoId) {
        Ciudadano cuenta = buscar(ciudadanoId);
        return new CondicionCiudadano(cuenta.isCorreoVerificado(), cuenta.requiereTelefono(),
                cuenta.suspensionVigente(Instant.now()), cuenta.getMotivoSuspension(), cuenta.getSuspendidaHasta());
    }

    /**
     * Nombre y teléfono descifrado para el personal de la organización responsable del reporte
     * (RF-038). Vacío si la cuenta se eliminó: el reporte queda desvinculado.
     */
    @Transactional(readOnly = true)
    public Optional<ContactoCiudadano> contacto(long ciudadanoId) {
        return ciudadanos.findById(ciudadanoId).map(c -> new ContactoCiudadano(c.getNombre(),
                c.getTelefonoCifrado() == null ? null : cifrador.descifrar(c.getTelefonoCifrado())));
    }

    /** Correo y nombre para los avisos de seguimiento (RF-044): solo si la cuenta los activó y su correo está verificado. */
    @Transactional(readOnly = true)
    public Optional<DestinatarioAviso> destinatarioAvisosCorreo(long ciudadanoId) {
        return ciudadanos.findById(ciudadanoId)
                .filter(c -> c.isAvisosCorreo() && c.isCorreoVerificado())
                .map(c -> new DestinatarioAviso(c.getCorreo(), c.getNombre()));
    }

    /** Solo cambia los campos enviados (null = sin cambio), como indica el contrato. */
    @Transactional
    public PerfilCiudadano actualizar(long ciudadanoId, String nombre, String telefono, Boolean avisosCorreo) {
        Ciudadano cuenta = buscar(ciudadanoId);
        if (nombre != null) {
            cuenta.cambiarNombre(nombre.strip());
        }
        if (telefono != null) {
            cuenta.cambiarTelefonoCifrado(cifrador.cifrar(telefono));
        }
        if (avisosCorreo != null) {
            cuenta.cambiarAvisosCorreo(avisosCorreo);
        }
        return perfilDe(cuenta);
    }

    /**
     * Elimina la cuenta (CU-25 FA-02). La base hace el resto: las suscripciones push se borran
     * en cascada y los reportes, intentos y señalamientos quedan con ciudadano_id en NULL.
     */
    @Transactional
    public void eliminar(long ciudadanoId) {
        ciudadanos.delete(buscar(ciudadanoId));
    }

    private Ciudadano buscar(long ciudadanoId) {
        // Una cuenta recién eliminada aún puede traer un token de acceso vigente unos minutos.
        return ciudadanos.findById(ciudadanoId)
                .orElseThrow(() -> new ReglaNegocioException(401, "NO_AUTENTICADO", "Vuelve a iniciar sesión."));
    }

    /** Muestra el estado efectivo: una suspensión ya vencida aparece como cuenta activa. */
    private PerfilCiudadano perfilDe(Ciudadano cuenta) {
        boolean suspendida = cuenta.suspensionVigente(Instant.now());
        PerfilCiudadano.Suspension suspension = suspendida
                ? new PerfilCiudadano.Suspension(cuenta.getMotivoSuspension(), cuenta.getSuspendidaHasta())
                : null;
        String telefono = cuenta.getTelefonoCifrado() == null ? null : cifrador.descifrar(cuenta.getTelefonoCifrado());
        return new PerfilCiudadano(cuenta.getNombre(), cuenta.getCorreo(), cuenta.getMetodoAcceso(),
                telefono, cuenta.isAvisosCorreo(), suspendida ? EstadoCuenta.suspendida : EstadoCuenta.activa,
                suspension);
    }
}
