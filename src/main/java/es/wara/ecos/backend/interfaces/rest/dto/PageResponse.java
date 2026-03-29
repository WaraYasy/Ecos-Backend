package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.Page;

import java.util.List;

/**
 * DTO de respuesta paginada genérico para los endpoints de ECOS.
 *
 * @param <T>      tipo de los elementos
 * @param items    elementos de la página actual
 * @param page     número de página actual (0-indexed)
 * @param pageSize tamaño de página
 * @param total    total de elementos que satisfacen los criterios de búsqueda
 * @param hasNext  {@code true} si existe al menos una página siguiente
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total,
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.items(), page.page(), page.pageSize(), page.total(), page.hasNext());
    }

}