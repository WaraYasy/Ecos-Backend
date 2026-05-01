package es.wara.ecos.backend.infrastructure.adapter.itunes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO raíz para la respuesta de la iTunes Search API.
 * Solo mapea los campos necesarios para la obtención de la preview URL;
 * el resto de campos se ignoran con {@code @JsonIgnoreProperties(ignoreUnknown = true)}.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItunesSearchResponseDTO {

    @JsonProperty("resultCount")
    private int resultCount;

    @JsonProperty("results")
    private List<ItunesTrackDTO> results;

    public int getResultCount() { return resultCount; }
    public List<ItunesTrackDTO> getResults() { return results; }

    /**
     * DTO para un track devuelto por iTunes, que solo expone la URL de preview de 30 segundos.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItunesTrackDTO {

        @JsonProperty("previewUrl")
        private String previewUrl;

        public String getPreviewUrl() { return previewUrl; }
    }
}