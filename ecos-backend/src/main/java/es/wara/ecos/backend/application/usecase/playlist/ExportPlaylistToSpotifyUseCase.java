package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.infrastructure.adapter.spotify.SpotifyAuthService;
import es.wara.ecos.backend.infrastructure.adapter.spotify.SpotifyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso para exportar una playlist de ECOS a la cuenta de Spotify del usuario.
 * <p>
 * Requiere que el usuario haya vinculado previamente su cuenta de Spotify mediante
 * el flujo Authorization Code ({@code GET /v1/auth/spotify/login}).
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@Service
public class ExportPlaylistToSpotifyUseCase {

    private final PlaylistRepository playlistRepository;
    private final SpotifyAuthService spotifyAuthService;
    private final SpotifyClient spotifyClient;

    public ExportPlaylistToSpotifyUseCase(
            PlaylistRepository playlistRepository,
            SpotifyAuthService spotifyAuthService,
            SpotifyClient spotifyClient
    ) {
        this.playlistRepository = playlistRepository;
        this.spotifyAuthService = spotifyAuthService;
        this.spotifyClient = spotifyClient;
    }

    /**
     * Exporta una playlist a Spotify creándola en la cuenta del usuario.
     *
     * @param playlistId identificador de la playlist en ECOS
     * @param userId     identificador del usuario que exporta (debe tener Spotify vinculado)
     * @return la playlist actualizada con el {@code spotifyExportId} asignado
     * @throws IllegalArgumentException si la playlist no existe o el usuario no ha vinculado Spotify
     */
    public Playlist execute(Long playlistId, Long userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist no encontrada: " + playlistId));

        String userToken = spotifyAuthService.getTokenForUser(userId);
        String spotifyUserId = spotifyClient.getCurrentSpotifyUser(userToken);

        String spotifyPlaylistId = spotifyClient.createPlaylist(
                userToken,
                spotifyUserId,
                playlist.getName(),
                playlist.getDescription()
        );

        List<String> trackUris = playlist.getSongs() != null
                ? playlist.getSongs().stream()
                        .filter(s -> s.getSpotifyId() != null)
                        .map(s -> "spotify:track:" + s.getSpotifyId())
                        .toList()
                : List.of();

        spotifyClient.addTracksToPlaylist(userToken, spotifyPlaylistId, trackUris);

        playlist.setSpotifyExportId(spotifyPlaylistId);
        Playlist saved = playlistRepository.save(playlist);

        log.info("Playlist {} exportada a Spotify con ID {}", playlistId, spotifyPlaylistId);
        return saved;
    }

}
