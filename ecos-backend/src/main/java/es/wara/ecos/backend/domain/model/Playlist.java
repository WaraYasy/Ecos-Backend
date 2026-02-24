package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa una lista de reproducción creada por un usuario en ECOS,
 * exportable a Spotify.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Playlist {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    /** ID de la playlist en Spotify si ha sido exportada; null en caso contrario. */
    private String spotifyExportId;
    private LocalDateTime createdAt;
    private List<Song> songs;

}
