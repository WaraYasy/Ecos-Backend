package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.application.usecase.search.SearchParams;
import es.wara.ecos.backend.domain.model.Page;
import es.wara.ecos.backend.domain.model.Song;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para el acceso y persistencia de canciones en el dominio de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface SongRepository {

    /**
     * Busca una canción por su identificador interno.
     *
     * @param id identificador interno de la canción
     * @return la canción encontrada, o vacío si no existe
     */
    Optional<Song> findById(UUID id);

    /**
     * Busca múltiples canciones por sus identificadores en una sola query IN.
     * Usar en lugar de N llamadas a {@link #findById} para evitar el problema N+1.
     *
     * @param ids lista de identificadores a buscar
     * @return lista de canciones encontradas (puede ser menor que ids si alguno no existe)
     */
    List<Song> findAllById(List<UUID> ids);

    /**
     * Busca una canción por su identificador de Spotify.
     *
     * @param spotifyId identificador único de Spotify
     * @return la canción encontrada, o vacío si no existe
     */
    Optional<Song> findBySpotifyId(String spotifyId);

    /**
     * Busca múltiples canciones por sus spotifyIds en una sola query IN.
     * Usar en lugar de N llamadas a {@link #findBySpotifyId} para evitar el problema N+1.
     *
     * @param spotifyIds lista de spotifyIds a buscar
     * @return lista de canciones encontradas (preserva solo las existentes en BD)
     */
    List<Song> findAllBySpotifyIdIn(List<String> spotifyIds);

    /**
     * Busca una canción por su código ISRC.
     *
     * @param isrc código ISRC de 12 caracteres (ej. USRC17607839)
     * @return la canción encontrada, o vacío si no existe
     */
    Optional<Song> findByIsrc(String isrc);

    /**
     * Persiste una canción en el repositorio.
     *
     * @param song canción a guardar
     * @return la canción guardada con su identificador asignado
     */
    Song save(Song song);

    /**
     * Busca canciones cuyo título contenga el texto indicado.
     *
     * @param title fragmento del título a buscar
     * @return lista de canciones que coinciden con el criterio
     */
    List<Song> findByTitleContaining(String title);

    /**
     * Búsqueda multidimensional usando los 5 cuadrantes de ECOS.
     *
     * @param params parámetros de búsqueda con cuadrantes activos y paginación
     * @return página de canciones que satisfacen todos los criterios activos
     */
    Page<Song> search(SearchParams params);

}
