package es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta de detalle de una grabación de MusicBrainz con sus release-groups.
 * <p>
 * Se deserializa desde {@code GET /ws/2/recording/{mbid}?inc=release-groups&fmt=json}.
 * Usado en el Flujo B para encontrar qué soundtracks (películas) contienen una grabación.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MusicBrainzRecordingDetailDTO {

    /** MBID único de la grabación. */
    private String id;

    private String title;

    /** Duración en milisegundos. Puede ser null. */
    private Integer length;

    @JsonProperty("artist-credit")
    private List<MusicBrainzArtistCreditDTO> artistCredit;

    /**
     * Release-groups a los que pertenece esta grabación.
     * Filtrar por {@code secondaryTypes} conteniendo "Soundtrack" para obtener películas.
     */
    @JsonProperty("release-groups")
    private List<MusicBrainzReleaseGroupDTO> releaseGroups;

}