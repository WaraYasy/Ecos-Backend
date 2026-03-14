package es.wara.ecos.backend.infrastructure.adapter.lastfm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa una etiqueta (tag) de la API de Last.fm.
 * <p>
 * Las etiquetas representan géneros, moods y estéticas asignadas por la comunidad.
 * El campo {@code count} indica la relevancia (0-100) del tag para esa canción.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LastFmTagDTO {

    /** Nombre del tag (género, mood o estética). */
    private String name;

    /** Relevancia del tag para la canción: 0-100. */
    private Integer count;

    /** URL de la página del tag en Last.fm. */
    private String url;
}