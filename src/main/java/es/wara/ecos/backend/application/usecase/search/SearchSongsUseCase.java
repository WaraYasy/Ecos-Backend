package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.application.service.MusicAggregationService;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Caso de uso para búsqueda multidimensional de canciones usando los cuadrantes de ECOS.
 *
 * <p>Implementa una estrategia <b>BD-primero</b>: consulta siempre la base de datos local y
 * solo recurre a APIs externas si no hay resultados y hay al menos un cuadrante activo.</p>
 *
 * <p>Jerarquía de fuentes externas: movie → aesthetic → texture → q/decade.
 * Solo una fuente trae canciones; los demás cuadrantes actúan como filtros en la re-query.</p>
 *
 * @author Wara Pacheco
 * @version 3.0
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

    public List<Song> execute(SearchParams params) {
        List<Song> songs = songRepository.search(params);

        if (!songs.isEmpty()) {
            return songs;
        }

        if (hasActiveQuadrant(params)) {
            List<Song> external = fetchFromExternalApis(params);

            List<CompletableFuture<Song>> futures = external.stream()
                    .map(song -> CompletableFuture.supplyAsync(() -> musicAggregationService.enrich(song)))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            futures.stream()
                    .map(CompletableFuture::join)
                    .peek(song -> ensureSearchTags(song, params))
                    .forEach(songRepository::save);

            songs = songRepository.search(params);
        }

        return songs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    /** Devuelve true si al menos un cuadrante activo puede disparar búsqueda externa. */
    private boolean hasActiveQuadrant(SearchParams params) {
        return (params.q() != null && !params.q().isBlank())
                || (params.aesthetic() != null && !params.aesthetic().isBlank())
                || (params.decade() != null && !params.decade().isBlank())
                || (params.movie() != null && !params.movie().isBlank())
                || (params.texture() != null && !params.texture().isBlank());
    }

    /**
     * Fetch con jerarquía: solo una fuente trae canciones, el resto son filtros.
     * movie → aesthetic → texture → q/decade
     */
    private List<Song> fetchFromExternalApis(SearchParams params) {
        int limit = params.limit();

        boolean hasMovie     = params.movie()     != null && !params.movie().isBlank();
        boolean hasAesthetic = params.aesthetic() != null && !params.aesthetic().isBlank();
        boolean hasTexture   = params.texture()   != null && !params.texture().isBlank();
        boolean hasTextOrDecade = (params.q() != null && !params.q().isBlank())
                               || (params.decade() != null && !params.decade().isBlank());

        if (hasMovie) {
            return new ArrayList<>(musicProvider.getSoundtrackSongs(params.movie()));
        }

        if (hasAesthetic) {
            // Si hay también decade/q, usar Spotify como fuente para respetar el filtro temporal.
            // ensureSearchTags añadirá el tag aesthetic a todas las canciones resultantes.
            if (hasTextOrDecade) {
                String spotifyQuery = buildSpotifyQuery(params);
                if (!spotifyQuery.isBlank()) {
                    return new ArrayList<>(musicProvider.searchSongs(spotifyQuery, limit));
                }
            }
            return new ArrayList<>(musicProvider.searchSongsByTag(params.aesthetic(), limit));
        }

        if (hasTexture) {
            if (hasTextOrDecade) {
                String spotifyQuery = buildSpotifyQuery(params);
                if (!spotifyQuery.isBlank()) {
                    return new ArrayList<>(musicProvider.searchSongs(spotifyQuery, limit));
                }
            }
            return new ArrayList<>(musicProvider.searchSongsByTag(params.texture(), limit));
        }

        if (hasTextOrDecade) {
            String spotifyQuery = buildSpotifyQuery(params);
            if (!spotifyQuery.isBlank()) {
                return new ArrayList<>(musicProvider.searchSongs(spotifyQuery, limit));
            }
        }

        return new ArrayList<>();
    }

    /**
     * Garantiza que las canciones tengan el tag del cuadrante por el que se buscaron,
     * independientemente de si Last.fm lo devolvió o no en el enriquecimiento.
     */
    private void ensureSearchTags(Song song, SearchParams params) {
        if (params.aesthetic() != null && !params.aesthetic().isBlank()) {
            addTagIfMissing(song, params.aesthetic().toLowerCase().trim());
        }
        if (params.texture() != null && !params.texture().isBlank()) {
            addTagIfMissing(song, params.texture().toLowerCase().trim());
        }
    }

    private void addTagIfMissing(Song song, String tagName) {
        List<Tag> current = song.getTags() != null ? new ArrayList<>(song.getTags()) : new ArrayList<>();
        boolean exists = current.stream().anyMatch(t -> tagName.equals(t.getName()));
        if (!exists) {
            Tag tag = new Tag();
            tag.setName(tagName);
            tag.setWeight(BigDecimal.ONE);
            current.add(tag);
            song.setTags(current);
        }
    }

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