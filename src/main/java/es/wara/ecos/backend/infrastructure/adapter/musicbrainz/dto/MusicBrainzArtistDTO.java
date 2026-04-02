package es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para un artista de MusicBrainz.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class MusicBrainzArtistDTO {

    /** MBID único del artista. */
    private String id;

    private String name;

    /** Nombre para ordenación alfabética, ej. "Zimmer, Hans". */
    @JsonProperty("sort-name")
    private String sortName;

    /** País o región de origen del artista. */
    private MusicBrainzAreaDTO area;

}
