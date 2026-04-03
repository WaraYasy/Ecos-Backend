package es.wara.ecos.backend.infrastructure.adapter.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta del endpoint {@code /search/multi} de TMDb.
 * Contiene películas, series y personas mezcladas. Filtrar por {@code media_type}.
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TMDBMultiSearchResponseDTO {

    private List<TMDBMultiResultDTO> results;
    private int page;

    @com.fasterxml.jackson.annotation.JsonProperty("total_results")
    private int totalResults;
}
