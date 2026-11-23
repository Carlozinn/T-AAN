package mx.uv.taan.administracion.aplicacion;

import mx.uv.taan.administracion.dominio.NivelParticipacion;

/** Esquema OrganizacionResumen del contrato. */
public record OrganizacionResumen(long id, String nombre, NivelParticipacion nivelParticipacion) {
}
