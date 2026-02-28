package es.wara.ecos.backend.infrastructure.adapter.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa la respuesta de {@code tag.getTopTracks} de la API de Last.fm.
 * <p>
 * Estructura de la respuesta:
 * <pre>
 * {
 *   "tracks": {
 *     "track": [
 *       {
 *         "name": "Blade Runner Blues",
 *         "duration": "630",
 *         "artist": { "name": "Vangelis" },
 *         "@attr": { "rank": "1" }
 *       }
 *     ]
 *   }
 * }
 * </pre>
 * </p>
 * <p>
 * Usado por el cuadrante de Estética (aesthetic=) del sistema de búsqueda:
 * dado un tag (ej. "cyberpunk"), devuelve las canciones más populares con ese tag.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LastFmTagTrackDTO {

    /** Contenedor de la lista de tracks del tag. */
    private TracksWrapper tracks;

    /**
     * Contenedor intermedio que agrupa la lista de tracks.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TracksWrapper {
        /** Lista de tracks ordenada por popularidad descendente dentro del tag. */
        private List<TagTrack> track;
    }

    /**
     * Datos de un track: título, duración y artista.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagTrack {
        /** Título del track. */
        private String name;

        /** Duración en segundos (puede ser "0" si Last.fm no la conoce). */
        private String duration;

        /** URL de la página del track en Last.fm. */
        private String url;

        /** Artista del track. */
        private TagTrackArtist artist;
    }

    /**
     * Artista simplificado del track obtenido por tag.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagTrackArtist {
        /** Nombre del artista. */
        private String name;
    }
}
