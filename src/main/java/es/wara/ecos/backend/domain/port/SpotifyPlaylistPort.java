package es.wara.ecos.backend.domain.port;

import java.util.List;

/**
 * Puerto de salida para operaciones de exportación de playlists a Spotify
 * mediante el Authorization Code Flow (token de usuario).
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface SpotifyPlaylistPort {

    /**
     * Obtiene el identificador de Spotify del usuario autenticado.
     *
     * @param userToken access token del usuario (Authorization Code Flow)
     * @return identificador de usuario en Spotify
     */
    String getCurrentUserId(String userToken);

    /**
     * Crea una playlist privada en la cuenta del usuario en Spotify.
     *
     * @param userToken      access token del usuario
     * @param spotifyUserId  identificador del usuario en Spotify
     * @param name           nombre de la playlist
     * @param description    descripción de la playlist
     * @return identificador de la playlist creada en Spotify
     */
    String createPlaylist(String userToken, String spotifyUserId, String name, String description);

    /**
     * Añade tracks a una playlist existente en Spotify.
     *
     * @param userToken         access token del usuario
     * @param spotifyPlaylistId identificador de la playlist en Spotify
     * @param trackUris         lista de URIs con formato {@code spotify:track:{id}}
     */
    void addTracksToPlaylist(String userToken, String spotifyPlaylistId, List<String> trackUris);

    /**
     * Sube una imagen de portada a una playlist existente en Spotify.
     * <p>
     * No es un error fatal si falla — la playlist ya tiene canciones y es válida sin portada.
     * </p>
     *
     * @param userToken         access token del usuario (Authorization Code Flow)
     * @param spotifyPlaylistId identificador de la playlist en Spotify
     * @param jpegBytes         bytes de la imagen en formato JPEG (máx. 256 KB)
     */
    void uploadPlaylistCover(String userToken, String spotifyPlaylistId, byte[] jpegBytes);

}
