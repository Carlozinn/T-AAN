package mx.uv.taan.cuentas.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.uv.taan.cuentas.dominio.LevantamientoSuspension;

/** Auditoría de levantamientos. Solo la usan los servicios del módulo cuentas. */
public interface LevantamientoSuspensionRepository extends JpaRepository<LevantamientoSuspension, Long> {
}
