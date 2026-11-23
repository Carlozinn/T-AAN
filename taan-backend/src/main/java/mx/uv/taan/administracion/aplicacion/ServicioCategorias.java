package mx.uv.taan.administracion.aplicacion;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.uv.taan.administracion.dominio.Categoria;
import mx.uv.taan.administracion.infraestructura.CategoriaRepository;
import mx.uv.taan.comun.error.ReglaNegocioException;

/**
 * Catálogo común de categorías y subcategorías (CU-14, RF-028). Dos niveles; el nombre no se
 * repite entre hermanas (sin distinguir mayúsculas). Desactivar no borra: los reportes ya
 * clasificados conservan su categoría.
 */
@Service
public class ServicioCategorias {

    private final CategoriaRepository categorias;

    public ServicioCategorias(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<CategoriaVista> listar() {
        return categorias.findAllByOrderByNombre().stream().map(CategoriaVista::de).toList();
    }

    /** Catálogo que usa el personal para confirmar o corregir la categoría (CU-06). */
    @Transactional(readOnly = true)
    public List<CategoriaVista> listarActivas() {
        return categorias.findByActivaTrueOrderByNombre().stream().map(CategoriaVista::de).toList();
    }

    /** Una categoría activa por id (para confirmar o corregir la clasificación, CU-06). */
    @Transactional(readOnly = true)
    public Optional<CategoriaVista> buscarActiva(long id) {
        return categorias.findById(id).filter(Categoria::isActiva).map(CategoriaVista::de);
    }

    @Transactional
    public CategoriaVista crear(String nombre, Long categoriaPadreId) {
        if (categoriaPadreId != null) {
            boolean padreValido = categorias.findById(categoriaPadreId).filter(Categoria::esRaiz).isPresent();
            if (!padreValido) {
                throw new ReglaNegocioException(400, "CATEGORIA_PADRE_INVALIDA",
                        "La categoría padre no existe o ya es una subcategoría.");
            }
        }
        String limpio = nombre.strip();
        if (existeHermana(limpio, categoriaPadreId, null)) {
            throw nombreDuplicado();
        }
        return CategoriaVista.de(categorias.save(new Categoria(limpio, categoriaPadreId)));
    }

    /** PATCH: null = sin cambio. */
    @Transactional
    public CategoriaVista actualizar(long id, String nombre, Boolean activa) {
        Categoria categoria = categorias.findById(id).orElseThrow(ReglaNegocioException::noEncontrado);
        if (nombre != null) {
            String limpio = nombre.strip();
            if (existeHermana(limpio, categoria.getCategoriaPadreId(), id)) {
                throw nombreDuplicado();
            }
            categoria.cambiarNombre(limpio);
        }
        if (activa != null) {
            categoria.cambiarActiva(activa);
        }
        return CategoriaVista.de(categoria);
    }

    private boolean existeHermana(String nombre, Long padreId, Long excluirId) {
        List<Categoria> hermanas = padreId == null
                ? categorias.findByCategoriaPadreIdIsNull()
                : categorias.findByCategoriaPadreId(padreId);
        return hermanas.stream()
                .anyMatch(c -> c.getNombre().equalsIgnoreCase(nombre) && !c.getId().equals(excluirId));
    }

    private static ReglaNegocioException nombreDuplicado() {
        return ReglaNegocioException.conflicto("NOMBRE_DUPLICADO", "Ya existe una categoría con ese nombre en ese nivel.");
    }
}
