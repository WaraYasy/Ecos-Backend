package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para exportar una playlist de ECOS a la cuenta de Spotify del usuario.
 * <p>
 * Requiere que el adaptador Spotify esté completamente implementado (tarea 2.9).
 * Actualmente lanza {@link UnsupportedOperationException}.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class ExportPlaylistToSpotifyUseCase {

    private final PlaylistRepository playlistRepository;

    public ExportPlaylistToSpotifyUseCase(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    /**
     * Exporta una playlist a Spotify.
     *
     * @param playlistId identificador de la playlist a exportar
     * @return la playlist actualizada con el ID de Spotify asignado
     * @throws IllegalArgumentException      si la playlist no existe
     * @throws UnsupportedOperationException hasta completar el adaptador Spotify (tarea 2.9)
     */
    public Playlist execute(Long playlistId) {
        playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist no encontrada: " + playlistId));

        // TODO (tarea 2.9): implementar SpotifyMusicProvider#createPlaylist y #addTracksToPlaylist
        throw new UnsupportedOperationException("Exportación a Spotify pendiente de implementar en tarea 2.9");
    }

}
