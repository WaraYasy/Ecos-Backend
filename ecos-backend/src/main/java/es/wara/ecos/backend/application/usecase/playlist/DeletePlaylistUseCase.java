package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para eliminar una playlist existente.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class DeletePlaylistUseCase {

    private final PlaylistRepository playlistRepository;

    public DeletePlaylistUseCase(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    /**
     * Elimina la playlist indicada.
     *
     * @param id identificador de la playlist a eliminar
     * @throws IllegalArgumentException si la playlist no existe
     */
    public void execute(Long id) {
        playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Playlist no encontrada: " + id));
        playlistRepository.delete(id);
    }

}
