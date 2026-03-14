package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Representa una canción en el dominio de ECOS, incluyendo sus características
 * de audio, etiquetas y metadatos de Spotify.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Song {

    private UUID id;
    private String spotifyId;
    /** Código ISRC (International Standard Recording Code, 12 chars). Puede ser null. */
    private String isrc;
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
