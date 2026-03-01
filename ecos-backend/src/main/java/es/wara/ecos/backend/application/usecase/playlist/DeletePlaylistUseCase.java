package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.Playlist;
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
     * Elimina la playlist indicada verificando que pertenezca al usuario actual.
     *
     * @param id            identificador de la playlist a eliminar
     * @param currentUserId identificador del usuario autenticado
     * @throws IllegalArgumentException si la playlist no existe
     * @throws ForbiddenException       si el usuario no es el propietario
     */
    public void execute(Long id, Long currentUserId) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Playlist no encontrada: " + id));
        if (!playlist.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("No tienes permiso para eliminar esta playlist");
        }
        playlistRepository.delete(id);
    }

}
