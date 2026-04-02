package es.wara.ecos.backend.infrastructure.adapter.deezer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Respuesta de la Deezer Search API: { "data": [...], "total": N, "next": "..." }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeezerSearchResponseDTO {

    private List<DeezerTrackDTO> data;

    public List<DeezerTrackDTO> getData() { return data; }
    public void setData(List<DeezerTrackDTO> data) { this.data = data; }
}
