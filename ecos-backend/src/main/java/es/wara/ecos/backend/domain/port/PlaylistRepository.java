package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.Playlist;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para el acceso y persistencia de playlists en el dominio de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface PlaylistRepository {

    /**
     * Busca una playlist por su identificador interno.
     *
     * @param id identificador interno de la playlist
     * @return la playlist encontrada, o vacío si no existe
     */
    Optional<Playlist> findById(Long id);

    /**
     * Obtiene todas las playlists de un usuario.
     *
     * @param userId identificador del usuario propietario
     * @return lista de playlists del usuario
     */
    List<Playlist> findByUserId(Long userId);

    /**
     * Persiste una playlist en el repositorio.
     *
     * @param playlist playlist a guardar
     * @return la playlist guardada con su identificador asignado
     */
    Playlist save(Playlist playlist);

    /**
     * Añade una canción a una playlist en la posición indicada.
     *
     * @param playlistId identificador de la playlist destino
     * @param songId     identificador de la canción a añadir
     * @param position   posición (basada en 1) donde se insertará la canción
     */
    void addSong(Long playlistId, Long songId, int position);

    /**
     * Elimina una playlist por su identificador.
     *
     * @param id identificador de la playlist a eliminar
     */
    void delete(Long id);

    /**
     * Elimina una canción de una playlist.
     *
     * @param playlistId identificador de la playlist
     * @param songId     identificador de la canción a eliminar
     */
    void removeSong(Long playlistId, Long songId);

    /**
     * Cuenta el número de canciones en una playlist.
     *
     * @param playlistId identificador de la playlist
     * @return número de canciones en la playlist
     */
    int countSongs(Long playlistId);

}
