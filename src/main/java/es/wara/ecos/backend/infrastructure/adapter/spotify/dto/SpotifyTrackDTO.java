package es.wara.ecos.backend.infrastructure.adapter.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa un track (canción) de la API de Spotify.
 * <p>
 * Se usa para deserializar respuestas de:
 * <ul>
 *   <li>{@code GET /v1/tracks/{id}}</li>
 *   <li>{@code GET /v1/search?type=track} (dentro de {@link SpotifySearchResponseDTO})</li>
 * </ul>
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyTrackDTO {

    /** Identificador único del track en Spotify. */
    private String id;

    /** Nombre del track. */
    private String name;

    /** Duración del track en milisegundos. */
    @JsonProperty("duration_ms")
    private Integer durationMs;

    /** URL de previsualización de 30 segundos (puede ser null). */
    @JsonProperty("preview_url")
    private String previewUrl;

    /** Popularidad del track en Spotify (0-100). */
    private Integer popularity;

    /** Álbum al que pertenece el track. */
    private AlbumSimple album;

    /** Lista de artistas que interpretan el track. */
    private List<ArtistSimple> artists;

    /**
     * Identificadores externos del track (ISRC, EAN, UPC).
     * Presente en respuestas de {@code GET /v1/tracks/{id}} y {@code GET /v1/search?type=track}.
     */
    @JsonProperty("external_ids")
    private ExternalIds externalIds;

    /**
     * Álbum simplificado incluido en la respuesta de track.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlbumSimple {
        private String id;
        private String name;

        /** Fecha de lanzamiento: "YYYY-MM-DD", "YYYY-MM" o "YYYY". */
        @JsonProperty("release_date")
        private String releaseDate;

        /** Imágenes de portada ordenadas de mayor a menor resolución. */
        private List<SpotifyImageDTO> images;
    }

    /**
     * Artista simplificado incluido en la respuesta de track.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArtistSimple {
        private String id;
        private String name;
    }

    /**
     * Identificadores externos del track.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExternalIds {
        /** International Standard Recording Code (12 caracteres, ej. USRC17607839). */
        private String isrc;
    }
}