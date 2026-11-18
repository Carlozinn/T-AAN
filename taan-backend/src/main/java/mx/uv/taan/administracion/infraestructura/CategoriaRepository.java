package mx.uv.taan.administracion.infraestructura;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.uv.taan.administracion.dominio.Categoria;

/**
 * Acceso al catálogo de categorías. Solo lo usan los servicios del módulo administración.
 * Las raíces y las hijas se piden por separado para no depender de cómo se traduce
 * «= null» en una consulta derivada.
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByNombre();

    List<Categoria> findByActivaTrueOrderByNombre();

    List<Categoria> findByCategoriaPadreIdIsNull();

    List<Categoria> findByCategoriaPadreId(Long categoriaPadreId);
}
