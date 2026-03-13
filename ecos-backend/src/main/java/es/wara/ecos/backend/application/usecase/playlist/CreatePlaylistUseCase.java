package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Caso de uso para crear una nueva playlist para un usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class CreatePlaylistUseCase {

    private final PlaylistRepository playlistRepository;

    public CreatePlaylistUseCase(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    /**
     * Crea y persiste una nueva playlist vacía.
     *
     * @param userId      identificador del usuario propietario
     * @param name        nombre de la playlist
     * @param description descripción opcional (puede ser null)
     * @return la playlist creada con su identificador asignado
     */
    public Playlist execute(UUID userId, String name, String description) {
        Playlist playlist = new Playlist();
        playlist.setUserId(userId);
        playlist.setName(name);
        playlist.setDescription(description);
        playlist.setCreatedAt(LocalDateTime.now());
        return playlistRepository.save(playlist);
    }

}
