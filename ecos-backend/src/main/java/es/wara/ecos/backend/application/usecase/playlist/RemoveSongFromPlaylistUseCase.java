package es.wara.ecos.backend.application.usecase.playlist;

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
     * Elimina una canción de la playlist indicada.
     *
     * @param playlistId identificador de la playlist
     * @param songId     identificador de la canción a eliminar
     * @throws IllegalArgumentException si la playlist no existe
     */
    public void execute(Long playlistId, Long songId) {
        playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist no encontrada: " + playlistId));
        playlistRepository.removeSong(playlistId, songId);
    }

}
