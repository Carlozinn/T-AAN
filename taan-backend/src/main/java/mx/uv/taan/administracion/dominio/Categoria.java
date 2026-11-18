package mx.uv.taan.administracion.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Categoría o subcategoría del catálogo común a todos los municipios (tabla categoria; RF-028,
 * CU-14). El catálogo tiene dos niveles: una subcategoría siempre cuelga de una categoría raíz.
 */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    /** Nulo en las categorías raíz. No cambia: mover una subcategoría alteraría el historial. */
    @Column(name = "categoria_padre_id", updatable = false)
    private Long categoriaPadreId;

    @Column(nullable = false)
    private boolean activa = true;

    protected Categoria() {
        // Requerido por JPA.
    }

    public Categoria(String nombre, Long categoriaPadreId) {
        this.nombre = nombre;
        this.categoriaPadreId = categoriaPadreId;
    }

    public boolean esRaiz() {
        return categoriaPadreId == null;
    }

    public void cambiarNombre(String nombre) {
        this.nombre = nombre;
    }

    /** Desactivar la retira de la clasificación nueva sin tocar los reportes ya clasificados. */
    public void cambiarActiva(boolean activa) {
        this.activa = activa;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Long getCategoriaPadreId() {
        return categoriaPadreId;
    }

    public boolean isActiva() {
        return activa;
    }
}
