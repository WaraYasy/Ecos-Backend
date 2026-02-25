package es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para un release-group de MusicBrainz (equivale a un álbum/OST).
 * Resultado de: GET /ws/2/release-group?query=secondarytype:soundtrack AND releasegroup:"título"
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class MusicBrainzReleaseGroupDTO {

    /** MBID único del release-group. */
    private String id;

    private String title;

    /** Fecha de la primera publicación, formato "YYYY-MM-DD" o "YYYY". */
    @JsonProperty("first-release-date")
    private String firstReleaseDate;

    /** Tipo principal: "Album", "Single", "EP". */
    @JsonProperty("primary-type")
    private String primaryType;

    /** Tipos secundarios: contiene "Soundtrack" para OSTs. */
    @JsonProperty("secondary-types")
    private List<String> secondaryTypes;

    @JsonProperty("artist-credit")
    private List<MusicBrainzArtistCreditDTO> artistCredit;

}
