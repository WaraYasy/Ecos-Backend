package es.wara.ecos.backend.infrastructure.adapter.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa la respuesta paginada de búsqueda de tracks en Spotify.
 * <p>
 * Se deserializa desde {@code GET /v1/search?type=track}.
 * Spotify envuelve los resultados en un objeto {@code tracks}.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifySearchResponseDTO {

    /** Página de resultados de tracks. */
    private TracksPage tracks;

    /**
     * Página paginada con los tracks encontrados.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TracksPage {

        /** Lista de tracks de la página actual. */
        private List<SpotifyTrackDTO> items;

        /** Total de resultados disponibles en Spotify. */
        private Integer total;

        /** Número máximo de resultados por página solicitado. */
        private Integer limit;

        /** Desplazamiento de la página actual (0-based). */
        private Integer offset;
    }
}