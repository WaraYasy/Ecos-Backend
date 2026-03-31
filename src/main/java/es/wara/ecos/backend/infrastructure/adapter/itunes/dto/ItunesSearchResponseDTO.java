package es.wara.ecos.backend.infrastructure.adapter.itunes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Respuesta de la iTunes Search API para búsquedas de canciones.
 *
 * @see <a href="https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI">iTunes Search API</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItunesSearchResponseDTO {

    @JsonProperty("resultCount")
    private int resultCount;

    @JsonProperty("results")
    private List<ItunesTrackDTO> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItunesTrackDTO {

        @JsonProperty("wrapperType")
        private String wrapperType;

        @JsonProperty("kind")
        private String kind;

        @JsonProperty("trackName")
        private String trackName;

        @JsonProperty("artistName")
        private String artistName;

        @JsonProperty("previewUrl")
        private String previewUrl;
    }
}