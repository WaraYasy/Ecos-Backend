package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.application.service.MusicAggregationService;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso para búsqueda multidimensional de canciones usando los 5 cuadrantes de ECOS.
 *
 * <p>Implementa una estrategia <b>BD-primero</b>: consulta siempre la base de datos local con
 * {@link SongRepository#search(SearchParams)} y solo recurre a APIs externas si no hay resultados
 * y hay al menos un cuadrante no-técnico activo. Las canciones nuevas se persisten para futuras
 * consultas.</p>
 *
 * <p>Si únicamente están activos cuadrantes técnicos (audio features), no se llama a ninguna
 * API externa — solo canciones ya enriquecidas en BD pueden satisfacer esos filtros.</p>
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
@Service
public class SearchSongsUseCase {

    private final SongRepository songRepository;
    private final MusicProvider musicProvider;
    private final MusicAggregationService musicAggregationService;

    public SearchSongsUseCase(SongRepository songRepository, MusicProvider musicProvider,
                               MusicAggregationService musicAggregationService) {
        this.songRepository = songRepository;
        this.musicProvider = musicProvider;
        this.musicAggregationService = musicAggregationService;
    }

    /**
     * Ejecuta la búsqueda multidimensional con estrategia BD-primero.
     *
     * @param params parámetros con los cuadrantes activos y paginación
     * @return lista de canciones que satisfacen todos los cuadrantes activos, nunca null
     */
    public List<Song> execute(SearchParams params) {
        // 1. Consultar siempre BD primero
        List<Song> songs = songRepository.search(params);

        if (!songs.isEmpty()) {
            return songs;
        }

        // 2. Sin resultados: si hay cuadrantes no-técnicos activos, buscar en APIs externas
        if (hasNonTechnicalQuadrant(params)) {
            List<Song> external = fetchFromExternalApis(params);
            external.stream()
                    .map(musicAggregationService::enrich)
                    .forEach(songRepository::save);

            // Re-query con los datos ya persistidos
            songs = songRepository.search(params);
        }

        // 3. Solo cuadrante técnico activo → devolver [] (no hay datos de audio features externos)
        return songs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    /** Devuelve true si al menos un cuadrante no-técnico tiene valor. */
    private boolean hasNonTechnicalQuadrant(SearchParams params) {
        return (params.q() != null && !params.q().isBlank())
                || (params.aesthetic() != null && !params.aesthetic().isBlank())
                || (params.decade() != null && !params.decade().isBlank())
                || (params.movie() != null && !params.movie().isBlank());
    }

    /**
     * Llama a las APIs externas según los cuadrantes no-técnicos activos y recoge todos los resultados.
     *
     * <ul>
     *   <li>Cinematográfico → {@link MusicProvider#getSoundtrackSongs(String)}</li>
     *   <li>Estética → {@link MusicProvider#searchSongsByTag(String, int)}</li>
     *   <li>Texto / Temporal → {@link MusicProvider#searchSongs(String, int)}</li>
     * </ul>
     */
    private List<Song> fetchFromExternalApis(SearchParams params) {
        List<Song> result = new ArrayList<>();
        int limit = params.limit();

        if (params.movie() != null && !params.movie().isBlank()) {
            result.addAll(musicProvider.getSoundtrackSongs(params.movie()));
        }

        if (params.aesthetic() != null && !params.aesthetic().isBlank()) {
            result.addAll(musicProvider.searchSongsByTag(params.aesthetic(), limit));
        }

        boolean hasTextOrDecade = (params.q() != null && !params.q().isBlank())
                || (params.decade() != null && !params.decade().isBlank());
        if (hasTextOrDecade) {
            String spotifyQuery = buildSpotifyQuery(params);
            if (!spotifyQuery.isBlank()) {
                result.addAll(musicProvider.searchSongs(spotifyQuery, limit));
            }
        }

        return result;
    }

    /**
     * Construye la query de Spotify combinando texto libre y filtro temporal.
     *
     * <p>Ejemplos:
     * <ul>
     *   <li>{@code q="Vangelis", decade="1980s"} → {@code "Vangelis year:1980-1989"}</li>
     *   <li>{@code decade="1990s"} → {@code "year:1990-1999"}</li>
     *   <li>{@code q="Blade Runner"} → {@code "Blade Runner"}</li>
     * </ul>
     * </p>
     */
    private String buildSpotifyQuery(SearchParams params) {
        StringBuilder sb = new StringBuilder();

        if (params.q() != null && !params.q().isBlank()) {
            sb.append(params.q().trim());
        }

        if (params.decade() != null && !params.decade().isBlank()) {
            try {
                int startYear = Integer.parseInt(params.decade().replace("s", "").trim());
                if (!sb.isEmpty()) sb.append(" ");
                sb.append("year:").append(startYear).append("-").append(startYear + 9);
            } catch (NumberFormatException ignored) {
                // Formato de decade inválido — ignorar
            }
        }

        return sb.toString();
    }
}
