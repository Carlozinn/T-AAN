package mx.uv.taan.administracion.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.uv.taan.administracion.dominio.Organizacion;

/** Acceso a las organizaciones. Solo lo usan los servicios del módulo administración. */
public interface OrganizacionRepository extends JpaRepository<Organizacion, Long> {

    List<Organizacion> findAllByOrderByNombre();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
