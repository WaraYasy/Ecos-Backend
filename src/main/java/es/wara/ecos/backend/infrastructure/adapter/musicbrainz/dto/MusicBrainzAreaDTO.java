package es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para el área geográfica de un artista en MusicBrainz.
 * Representa el país o región de origen del artista.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class MusicBrainzAreaDTO {

    /** MBID del área. */
    private String id;

    /** Nombre del área en inglés (ej. "United Kingdom", "Japan", "Spain"). */
    private String name;

    /** Código(s) ISO 3166-1 alpha-2 del país (ej. ["GB"], ["JP"], ["ES"]). */
    @JsonProperty("iso-3166-1-codes")
    private List<String> iso31661Codes;
}
