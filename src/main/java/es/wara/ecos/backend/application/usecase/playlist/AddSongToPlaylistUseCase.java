package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso para añadir una canción al final de una playlist existente.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class AddSongToPlaylistUseCase {

    private final PlaylistRepository playlistRepository;
    private final SongRepository songRepository;

    public AddSongToPlaylistUseCase(PlaylistRepository playlistRepository, SongRepository songRepository) {
        this.playlistRepository = playlistRepository;
        this.songRepository = songRepository;
    }

    /**
     * Añade una canción al final de la playlist indicada verificando que pertenezca al usuario actual.
     *
     * @param playlistId    identificador de la playlist destino
     * @param songId        identificador de la canción a añadir
     * @param currentUserId identificador del usuario autenticado
     * @throws IllegalArgumentException si la playlist o la canción no existen
     * @throws ForbiddenException       si el usuario no es el propietario
     */
    public void execute(UUID playlistId, UUID songId, UUID currentUserId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist no encontrada: " + playlistId));
        if (!playlist.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("No tienes permiso para modificar esta playlist");
        }

        songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Canción no encontrada: " + songId));

        int nextPosition = playlistRepository.countSongs(playlistId) + 1;
        playlistRepository.addSong(playlistId, songId, nextPosition);
    }

}
