package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.Song;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para el acceso y persistencia de canciones en el dominio de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface SongRepository {

    /**
     * Busca una canción por su identificador interno.
     *
     * @param id identificador interno de la canción
     * @return la canción encontrada, o vacío si no existe
     */
    Optional<Song> findById(Long id);

    /**
     * Busca una canción por su identificador de Spotify.
     *
     * @param spotifyId identificador único de Spotify
     * @return la canción encontrada, o vacío si no existe
     */
    Optional<Song> findBySpotifyId(String spotifyId);

    /**
     * Persiste una canción en el repositorio.
     *
     * @param song canción a guardar
     * @return la canción guardada con su identificador asignado
     */
    Song save(Song song);

    /**
     * Busca canciones cuyo título contenga el texto indicado.
     *
     * @param title fragmento del título a buscar
     * @return lista de canciones que coinciden con el criterio
     */
    List<Song> findByTitleContaining(String title);

}
