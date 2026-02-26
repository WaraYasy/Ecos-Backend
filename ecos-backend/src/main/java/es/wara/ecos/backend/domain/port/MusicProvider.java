package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.AudioFeatures;
import es.wara.ecos.backend.domain.model.Movie;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida hacia proveedores externos de música (Spotify, Last.fm),
 * abstrayendo la obtención de canciones, características de audio y etiquetas.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface MusicProvider {

    /**
     * Busca canciones en el proveedor externo que coincidan con la consulta.
     *
     * @param query texto de búsqueda (título, artista o álbum)
     * @param limit número máximo de resultados a devolver
     * @return lista de canciones que coinciden con la búsqueda
     */
    List<Song> searchSongs(String query, int limit);

    /**
     * Obtiene las características de audio de una canción desde Spotify.
     *
     * @param spotifyId identificador único de la canción en Spotify
     * @return las características de audio, o vacío si no están disponibles
     */
    Optional<AudioFeatures> getAudioFeatures(String spotifyId);

    /**
     * Obtiene canciones similares a la indicada desde el proveedor externo.
     *
     * @param artist nombre del artista de la canción de referencia
     * @param title  título de la canción de referencia
     * @param limit  número máximo de canciones similares a devolver
     * @return lista de canciones similares
     */
    List<Song> getSimilarSongs(String artist, String title, int limit);

    /**
     * Obtiene las etiquetas de género y estilo asociadas a una canción.
     *
     * @param artist nombre del artista
     * @param title  título de la canción
     * @return lista de etiquetas con su peso de relevancia
     */
    List<Tag> getSongTags(String artist, String title);

    /**
     * Obtiene todas las canciones del soundtrack de una película (Flujo A).
     * <p>
     * Flujo interno: MusicBrainz (tracklist OST) → Spotify (enriquecer cada pista).
     * Si no hay OST oficial en MusicBrainz, intenta fallback por tag del título.
     * </p>
     *
     * @param movieTitle título de la película
     * @return lista de canciones del soundtrack; vacía si no se encontró OST
     */
    List<Song> getSoundtrackSongs(String movieTitle);

    /**
     * Obtiene las películas a las que pertenece una canción (Flujo B).
     * <p>
     * Flujo interno: MusicBrainz (grabación → soundtracks) → TMDb (metadatos de película).
     * </p>
     *
     * @param artist nombre del artista
     * @param title  título de la canción
     * @return lista de películas en las que aparece la canción; vacía si no se encontró ninguna
     */
    List<Movie> getMoviesForSong(String artist, String title);

}
