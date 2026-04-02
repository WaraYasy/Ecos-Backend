package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.AudioFeatures;
import es.wara.ecos.backend.domain.model.Movie;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida hacia proveedores externos de música.
 * <p>
 * Abstrae las cuatro fuentes externas de ECOS para el dominio:
 * </p>
 * <ul>
 *   <li><b>Spotify</b>: búsqueda de canciones, audio features, previews</li>
 *   <li><b>Last.fm</b>: tags de género/mood/estética, tracks similares, top tracks por tag</li>
 *   <li><b>MusicBrainz</b>: OSTs de películas (Flujo A), películas de una grabación (Flujo B),
 *       artistas por país de origen (cuadrante geográfico)</li>
 *   <li><b>TMDb</b>: metadatos de películas (título, año, director, géneros)</li>
 * </ul>
 *
 * @author Wara Pacheco
 * @version 1.1
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

    /**
     * Busca canciones asociadas a una etiqueta estética / género en Last.fm y las enriquece con Spotify.
     * <p>
     * Flujo: Last.fm {@code tag.getTopTracks} → por cada track, buscar en Spotify para obtener
     * spotifyId, previewUrl y datos de álbum/artista. Si Spotify no encuentra el track, se omite.
     * </p>
     *
     * @param tag   etiqueta estética o de género (ej. "cyberpunk", "dreamy", "cottagecore")
     * @param limit número máximo de canciones a devolver
     * @return lista de canciones enriquecidas con Spotify; vacía si no hay resultados o hay error
     */
    List<Song> searchSongsByTag(String tag, int limit);

    /**
     * Busca artistas cuyo país de origen coincide con el dado y devuelve sus nombres.
     * <p>
     * Fuente: MusicBrainz (búsqueda por área geográfica). Se usa como primer paso
     * del cuadrante geográfico para luego buscar canciones de esos artistas.
     * </p>
     *
     * @param country nombre del país en inglés (ej. "Spain", "Japan")
     * @param limit   máximo de artistas a devolver
     * @return lista de nombres de artistas del país; vacía si no hay resultados
     */
    List<String> getArtistNamesByCountry(String country, int limit);

    /**
     * Busca una URL de preview de 30s en iTunes para la canción indicada.
     * <p>
     * Se usa como fuente alternativa a Spotify, que dejó de devolver {@code preview_url}
     * en Development Mode a partir de noviembre de 2024.
     * </p>
     *
     * @param title  título de la canción
     * @param artist nombre del artista principal
     * @return URL del preview, o {@link Optional#empty()} si no se encontró
     */
    Optional<String> findPreviewUrl(String title, String artist);

}
