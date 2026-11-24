package mx.uv.taan.informes.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.uv.taan.informes.dominio.InformeGenerado;

/** Registro de informes generados. Solo lo usan los servicios del módulo informes. */
public interface InformeGeneradoRepository extends JpaRepository<InformeGenerado, Long> {
}
