package es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el crédito de artista en un release o recording de MusicBrainz.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class MusicBrainzArtistCreditDTO {

    /** Nombre tal como aparece en el crédito (puede diferir del nombre canónico). */
    private String name;

    /** Texto entre artistas, ej. " feat. ", " & ". Vacío para el último artista. */
    private String joinphrase;

    private MusicBrainzArtistDTO artist;

}
