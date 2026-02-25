package es.wara.ecos.backend.infrastructure.adapter.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa la respuesta de {@code track.getTopTags} de la API de Last.fm.
 * <p>
 * Estructura de la respuesta:
 * <pre>
 * {
 *   "toptags": {
 *     "tag": [ {"name": "rock", "count": 100, "url": "..."}, ... ]
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
public class LastFmTrackInfoDTO {

    /** Contenedor de los top tags del track. */
    private TopTagsWrapper toptags;

    /**
     * Contenedor intermedio que agrupa la lista de tags.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopTagsWrapper {
        /** Lista de tags ordenada por relevancia descendente. */
        private List<LastFmTagDTO> tag;
    }
}