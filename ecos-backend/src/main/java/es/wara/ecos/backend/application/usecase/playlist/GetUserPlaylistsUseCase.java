package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso para obtener todas las playlists de un usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class GetUserPlaylistsUseCase {

    private final PlaylistRepository playlistRepository;

    public GetUserPlaylistsUseCase(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    /**
     * Devuelve las playlists del usuario indicado.
     *
     * @param userId identificador del usuario
     * @return lista de playlists del usuario (puede estar vacía)
     */
    public List<Playlist> execute(Long userId) {
        return playlistRepository.findByUserId(userId);
    }

}
