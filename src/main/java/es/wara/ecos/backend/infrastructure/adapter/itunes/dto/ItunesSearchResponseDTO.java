package es.wara.ecos.backend.infrastructure.adapter.itunes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ItunesSearchResponseDTO {

    @JsonProperty("resultCount")
    private int resultCount;

    @JsonProperty("results")
    private List<ItunesTrackDTO> results;

    public int getResultCount() { return resultCount; }
    public List<ItunesTrackDTO> getResults() { return results; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItunesTrackDTO {

        @JsonProperty("previewUrl")
        private String previewUrl;

        public String getPreviewUrl() { return previewUrl; }
    }
}