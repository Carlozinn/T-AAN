package mx.uv.taan.configuracion.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.uv.taan.configuracion.dominio.Area;

/** Acceso a las áreas. Solo lo usan los servicios del módulo configuración. */
public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findByOrganizacionIdOrderByNombre(Long organizacionId);

    List<Area> findByOrganizacionIdAndActivaTrueOrderByNombre(Long organizacionId);

    Optional<Area> findByIdAndOrganizacionId(Long id, Long organizacionId);

    boolean existsByOrganizacionIdAndNombreIgnoreCase(Long organizacionId, String nombre);

    boolean existsByOrganizacionIdAndNombreIgnoreCaseAndIdNot(Long organizacionId, String nombre, Long id);
}
