package mx.uv.taan.administracion.aplicacion;

import mx.uv.taan.administracion.dominio.Categoria;

/** Esquema Categoria del contrato. categoriaPadreId es null en las categorías raíz. */
public record CategoriaVista(long id, String nombre, Long categoriaPadreId, boolean activa) {

    static CategoriaVista de(Categoria c) {
        return new CategoriaVista(c.getId(), c.getNombre(), c.getCategoriaPadreId(), c.isActiva());
    }
}
