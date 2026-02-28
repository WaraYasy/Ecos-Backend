package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para obtener una playlist por su identificador.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class GetPlaylistByIdUseCase {

    private final PlaylistRepository playlistRepository;

    public GetPlaylistByIdUseCase(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    /**
     * Devuelve la playlist con el identificador dado.
     *
     * @param id identificador de la playlist
     * @return la playlist encontrada
     * @throws IllegalArgumentException si la playlist no existe
     */
    public Playlist execute(Long id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Playlist no encontrada: " + id));
    }

}
