package es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para una edición (release) concreta de un álbum en MusicBrainz.
 * Resultado de: GET /ws/2/release/{mbid}?inc=recordings+artist-credits
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class MusicBrainzReleaseDTO {

    /** MBID único de esta edición. */
    private String id;

    private String title;

    /** "Official", "Promotion", "Bootleg". Preferir "Official". */
    private String status;

    private String date;

    private String country;

    /** Lista de discos/medios que componen el release. Normalmente uno solo. */
    private List<MusicBrainzMediumDTO> media;

}
