package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa una canción en el dominio de ECOS, incluyendo sus características
 * de audio, etiquetas y metadatos de Spotify.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Song {

    private Long id;
    private String spotifyId;
    private String title;
    /** Duración en milisegundos. */
    private Integer durationMs;
    private String previewUrl;
    private Album album;
    private List<Artist> artists;
    private AudioFeatures audioFeatures;
    private List<Tag> tags;
    /** Películas en las que aparece la canción (fuente: TMDb vía MusicBrainz). */
    private List<Movie> movies;
    private LocalDateTime createdAt;

}
