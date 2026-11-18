package mx.uv.taan.administracion.infraestructura;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.uv.taan.administracion.dominio.Regla;

/** Acceso a las reglas de canalización. Solo lo usan los servicios del módulo administración. */
public interface ReglaRepository extends JpaRepository<Regla, Long> {

    List<Regla> findAllByOrderByZonaIdAscCategoriaIdAscPrioridadDesc();

    List<Regla> findByZonaIdOrderByCategoriaIdAscPrioridadDesc(Long zonaId);

    List<Regla> findByOrganizacionIdAndActivaTrue(Long organizacionId);

    Optional<Regla> findFirstByZonaIdAndCategoriaIdAndOrganizacionIdAndActivaTrue(
            Long zonaId, Long categoriaId, Long organizacionId);

    boolean existsByZonaIdAndCategoriaIdAndOrganizacionIdAndActivaTrue(
            Long zonaId, Long categoriaId, Long organizacionId);

    boolean existsByZonaIdAndOrganizacionIdAndActivaTrue(Long zonaId, Long organizacionId);
}
