package es.wara.ecos.backend.infrastructure.adapter.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa la respuesta paginada de búsqueda de películas en TMDb.
 * <p>
 * Se deserializa desde {@code GET /search/movie?query=...}.
 * TMDb devuelve 20 resultados por página.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TMDBSearchResponseDTO {

    /** Número de página actual (1-based). */
    private Integer page;

    /** Lista de películas encontradas en esta página. */
    private List<TMDBMovieDTO> results;

    /** Total de páginas disponibles. */
    @JsonProperty("total_pages")
    private Integer totalPages;

    /** Total de resultados en todas las páginas. */
    @JsonProperty("total_results")
    private Integer totalResults;
}