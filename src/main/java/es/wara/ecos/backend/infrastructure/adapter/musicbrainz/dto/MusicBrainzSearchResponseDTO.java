package es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO raíz para las respuestas de búsqueda de MusicBrainz.
 * Cubre búsquedas de release-groups, releases, recordings y artistas.
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class MusicBrainzSearchResponseDTO {

    /** Total de resultados encontrados (puede ser mayor que el límite pedido). */
    private Integer count;

    /** Offset de la página actual. */
    private Integer offset;

    /** Resultados de búsqueda de release-groups (OSTs). */
    @JsonProperty("release-groups")
    private List<MusicBrainzReleaseGroupDTO> releaseGroups;

    /** Resultados de búsqueda de releases. */
    private List<MusicBrainzReleaseDTO> releases;

    /** Resultados de búsqueda de recordings (fallback por tags). */
    private List<MusicBrainzRecordingDTO> recordings;

    /** Resultados de búsqueda de artistas por área geográfica. */
    private List<MusicBrainzArtistDTO> artists;

}
