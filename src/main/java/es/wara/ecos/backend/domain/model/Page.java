package es.wara.ecos.backend.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Resultado paginado genérico del dominio de ECOS.
 *
 * @param <T>      tipo de los elementos
 * @param items    elementos de la página actual
 * @param page     número de página actual (0-indexed)
 * @param pageSize tamaño de página
 * @param total    total de elementos que satisfacen los criterios
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record Page<T>(List<T> items, int page, int pageSize, long total) {

    /**
     * Indica si existe una página siguiente con más resultados.
     *
     * @return {@code true} si hay más elementos más allá de la página actual
     */
    public boolean hasNext() {
        return (long) (page + 1) * pageSize < total;
    }

    /**
     * Transforma los elementos de la página aplicando la función dada, conservando la paginación.
     *
     * @param <R>    tipo del resultado de la transformación
     * @param mapper función de transformación de T a R
     * @return nueva página con los elementos transformados y los mismos metadatos de paginación
     */
    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), page, pageSize, total);
    }

}