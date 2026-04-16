package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.Playlist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    Optional<Playlist> findById(UUID id);

    /**
     * Obtiene todas las playlists de un usuario.
     *
     * @param userId identificador del usuario propietario
     * @return lista de playlists del usuario
     */
    List<Playlist> findByUserId(UUID userId);

    /**
     * Persiste una playlist en el repositorio.
     *
     * @param playlist playlist a guardar
     * @return la playlist guardada con su identificador asignado
     */
    Playlist save(Playlist playlist);

    /**
     * Añade una canción al final de una playlist.
     * La posición se calcula de forma atómica en la BD (MAX + 1),
     * eliminando la race condition del patrón read-modify-write.
     * Si la canción ya existe en la playlist, la operación se ignora.
     *
     * @param playlistId identificador de la playlist destino
     * @param songId     identificador de la canción a añadir
     */
    void addSong(UUID playlistId, UUID songId);

    /**
     * Elimina una playlist por su identificador.
     *
     * @param id identificador de la playlist a eliminar
     */
    void delete(UUID id);

    /**
     * Elimina una canción de una playlist.
     *
     * @param playlistId identificador de la playlist
     * @param songId     identificador de la canción a eliminar
     */
    void removeSong(UUID playlistId, UUID songId);

}
