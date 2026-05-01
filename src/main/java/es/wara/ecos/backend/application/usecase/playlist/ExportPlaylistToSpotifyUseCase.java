package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
import es.wara.ecos.backend.domain.port.SpotifyPlaylistPort;
import es.wara.ecos.backend.domain.port.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para exportar una playlist de ECOS a la cuenta de Spotify del usuario.
 * <p>
 * Requiere que el usuario haya vinculado previamente su cuenta de Spotify mediante
 * el flujo Authorization Code ({@code GET /v1/auth/spotify/login}).
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
@Slf4j
@Service
public class ExportPlaylistToSpotifyUseCase {

    private static final String DESC_ES = "Desde el universo de ECOS";
    private static final String DESC_EN = "From the ECOS universe";

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final SpotifyAuthPort spotifyAuthPort;
    private final SpotifyPlaylistPort spotifyPlaylistPort;

    private byte[] coverImageBytes;

    public ExportPlaylistToSpotifyUseCase(
            PlaylistRepository playlistRepository,
            UserRepository userRepository,
            SpotifyAuthPort spotifyAuthPort,
            SpotifyPlaylistPort spotifyPlaylistPort
    ) {
        this.playlistRepository  = playlistRepository;
        this.userRepository      = userRepository;
        this.spotifyAuthPort     = spotifyAuthPort;
        this.spotifyPlaylistPort = spotifyPlaylistPort;
    }

    @PostConstruct
    void loadCoverImage() {
        try {
            ClassPathResource resource = new ClassPathResource("static/jpg/ecos-cover.jpg");
            coverImageBytes = resource.getInputStream().readAllBytes();
            log.info("Imagen de portada ECOS cargada ({} bytes)", coverImageBytes.length);
        } catch (IOException e) {
            log.warn("No se pudo cargar la imagen de portada de ECOS: {}", e.getMessage());
        }
    }

    /**
     * Exporta una playlist a Spotify creándola en la cuenta del usuario.
     *
     * @param playlistId identificador de la playlist en ECOS
     * @param userId     identificador del usuario que exporta (debe tener Spotify vinculado)
     * @return la playlist actualizada con el {@code spotifyExportId} asignado
     * @throws IllegalArgumentException si la playlist o el usuario no existen, o el usuario no ha vinculado Spotify
     */
    public Playlist execute(UUID playlistId, UUID userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist no encontrada: " + playlistId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        String description = (playlist.getDescription() != null && !playlist.getDescription().isBlank())
                ? playlist.getDescription()
                : resolveDescription(user.getLanguage());

        String userToken = spotifyAuthPort.getTokenForUser(userId);
        String spotifyUserId = spotifyPlaylistPort.getCurrentUserId(userToken);

        String spotifyPlaylistId = spotifyPlaylistPort.createPlaylist(
                userToken,
                spotifyUserId,
                playlist.getName(),
                description
        );

        List<String> trackUris = playlist.getSongs() != null
                ? playlist.getSongs().stream()
                        .filter(s -> s.getSpotifyId() != null)
                        .map(s -> "spotify:track:" + s.getSpotifyId())
                        .toList()
                : List.of();

        spotifyPlaylistPort.addTracksToPlaylist(userToken, spotifyPlaylistId, trackUris);

        if (coverImageBytes != null) {
            spotifyPlaylistPort.uploadPlaylistCover(userToken, spotifyPlaylistId, coverImageBytes);
        }

        playlist.setSpotifyExportId(spotifyPlaylistId);
        playlistRepository.save(playlist);

        log.info("Playlist {} exportada a Spotify con ID {}", playlistId, spotifyPlaylistId);
        return playlistRepository.findById(playlistId).orElse(playlist);
    }

    private String resolveDescription(String language) {
        if (language != null && language.toLowerCase().startsWith("en")) {
            return DESC_EN;
        }
        return DESC_ES;
    }

}