package mx.uv.taan.administracion.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.uv.taan.administracion.dominio.CanalExterno;

/** Acceso a los canales oficiales externos. Solo lo usan los servicios del módulo administración. */
public interface CanalExternoRepository extends JpaRepository<CanalExterno, Long> {

    List<CanalExterno> findAllByOrderByOrganizacionIdAscIdAsc();

    /** Los que se muestran al ciudadano en canalización externa (incremento 3). */
    List<CanalExterno> findByOrganizacionIdAndActivoTrue(Long organizacionId);
}
