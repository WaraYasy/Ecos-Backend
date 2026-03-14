package es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para una grabación (recording) de MusicBrainz.
 * Una grabación es la versión única de una canción; puede aparecer en varios releases.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class MusicBrainzRecordingDTO {

    /** MBID único de la grabación. */
    private String id;

    private String title;

    /** Duración en milisegundos. Puede ser null. */
    private Integer length;

    @JsonProperty("first-release-date")
    private String firstReleaseDate;

    @JsonProperty("artist-credit")
    private List<MusicBrainzArtistCreditDTO> artistCredit;

}
