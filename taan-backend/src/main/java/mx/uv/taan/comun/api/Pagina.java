package mx.uv.taan.comun.api;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Página de resultados con la forma de los esquemas Pagina* del contrato
 * (contenido, pagina, tamano, totalElementos, totalPaginas).
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <E, T> Pagina<T> de(Page<E> pagina, Function<E, T> convertir) {
        return new Pagina<>(pagina.getContent().stream().map(convertir).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
