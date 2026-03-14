package es.wara.ecos.backend.infrastructure.adapter.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa la respuesta de {@code track.getSimilar} de la API de Last.fm.
 * <p>
 * Estructura de la respuesta:
 * <pre>
 * {
 *   "similartracks": {
 *     "track": [
 *       { "name": "...", "match": 0.9, "artist": { "name": "..." } }
 *     ]
 *   }
 * }
 * </pre>
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LastFmSimilarTracksDTO {

    /** Contenedor de los tracks similares. */
    private SimilarTracksWrapper similartracks;

    /**
     * Contenedor intermedio que agrupa la lista de tracks similares.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SimilarTracksWrapper {
        /** Lista de tracks similares ordenada por similitud descendente. */
        private List<SimilarTrack> track;
    }

    /**
     * Datos de un track similar: título, puntuación de similitud y artista.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SimilarTrack {
        /** Título del track. */
        private String name;

        /** Puntuación de similitud respecto al track de referencia (0.0-1.0). */
        private Double match;

        /** URL de la página del track en Last.fm. */
        private String url;

        /** Artista del track similar. */
        private SimilarArtist artist;
    }

    /**
     * Artista simplificado del track similar.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SimilarArtist {
        /** Nombre del artista. */
        private String name;
    }
}