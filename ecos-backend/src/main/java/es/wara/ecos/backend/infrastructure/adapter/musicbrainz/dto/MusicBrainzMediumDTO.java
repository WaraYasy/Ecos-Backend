package es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para un medio (disco) dentro de un release de MusicBrainz.
 * Contiene la lista de pistas del disco.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class MusicBrainzMediumDTO {

    /** Número de disco, comenzando en 1. */
    private Integer position;

    /** Formato físico: "CD", "Digital Media", "Vinyl", etc. */
    private String format;

    @JsonProperty("track-count")
    private Integer trackCount;

    private List<MusicBrainzTrackDTO> tracks;

}
