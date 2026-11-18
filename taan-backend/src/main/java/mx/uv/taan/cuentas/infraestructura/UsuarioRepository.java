package mx.uv.taan.cuentas.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.uv.taan.cuentas.dominio.Usuario;

/** Acceso a los usuarios institucionales. Solo lo usan los servicios del módulo cuentas. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    List<Usuario> findByOrganizacionIdOrderByNombre(Long organizacionId);

    Optional<Usuario> findByIdAndOrganizacionId(Long id, Long organizacionId);

    boolean existsByAreaIdAndActivoTrue(Long areaId);
}
