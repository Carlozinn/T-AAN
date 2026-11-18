package mx.uv.taan.cuentas.aplicacion;

import java.time.Instant;

import mx.uv.taan.cuentas.dominio.EstadoCuenta;
import mx.uv.taan.cuentas.dominio.MetodoAcceso;

/**
 * Esquema Perfil del contrato. telefono y suspension se envían como null cuando no aplican
 * (el contrato los marca nullable). El teléfono ya va descifrado: solo lo ve su dueño.
 */
public record PerfilCiudadano(
        String nombre,
        String correo,
        MetodoAcceso metodoAcceso,
        String telefono,
        boolean avisosCorreo,
        EstadoCuenta estado,
        Suspension suspension) {

    /** hasta = null significa suspensión indefinida (RF-043). */
    public record Suspension(String motivo, Instant hasta) {
    }
}
