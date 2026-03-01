package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para eliminar una canción de una playlist.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class RemoveSongFromPlaylistUseCase {

    private final PlaylistRepository playlistRepository;

    public RemoveSongFromPlaylistUseCase(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    /**
     * Elimina una canción de la playlist indicada verificando que pertenezca al usuario actual.
     *
     * @param playlistId    identificador de la playlist
     * @param songId        identificador de la canción a eliminar
     * @param currentUserId identificador del usuario autenticado
     * @throws IllegalArgumentException si la playlist no existe
     * @throws ForbiddenException       si el usuario no es el propietario
     */
    public void execute(Long playlistId, Long songId, Long currentUserId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist no encontrada: " + playlistId));
        if (!playlist.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("No tienes permiso para modificar esta playlist");
        }
        playlistRepository.removeSong(playlistId, songId);
    }

}
