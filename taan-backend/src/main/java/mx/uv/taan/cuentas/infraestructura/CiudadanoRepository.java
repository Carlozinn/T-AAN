package mx.uv.taan.cuentas.infraestructura;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.uv.taan.cuentas.dominio.Ciudadano;

/** Acceso a las cuentas ciudadanas. Solo lo usan los servicios del módulo cuentas. */
public interface CiudadanoRepository extends JpaRepository<Ciudadano, Long> {

    Optional<Ciudadano> findByCorreo(String correo);

    Optional<Ciudadano> findByGoogleSub(String googleSub);
}
