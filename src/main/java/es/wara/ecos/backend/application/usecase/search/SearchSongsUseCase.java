package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.application.service.MusicAggregationService;
import es.wara.ecos.backend.domain.model.Page;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Caso de uso para búsqueda multidimensional de canciones usando los 5 cuadrantes de ECOS.
 *
 * <p><b>Estrategia BD-primero:</b> consulta siempre la base de datos local y solo recurre
 * a APIs externas si no hay resultados y hay al menos un cuadrante activo. Tras enriquecer
 * y persistir los resultados externos, re-ejecuta la búsqueda en BD para devolver datos
 * coherentes con el modelo de respuesta.</p>
 *
 * <p><b>Jerarquía de fuentes externas:</b> {@code movie → aesthetic → texture → q/decade}.
 * Solo una fuente trae canciones; los demás cuadrantes actúan como filtros en la re-query.
 * Excepción: si {@code aesthetic} y {@code decade/q} están activos simultáneamente, se usa
 * Spotify con filtro temporal como fuente primaria para garantizar que la re-query encuentre
 * canciones que satisfagan ambos cuadrantes. {@link #ensureSearchTags} añade el tag aesthetic
 * a los resultados.</p>
 *
 * <p><b>Tags garantizados:</b> {@link #ensureSearchTags} asegura que todas las canciones
 * devueltas tengan el tag del cuadrante por el que se buscó, independientemente de si
 * Last.fm lo devolvió durante el enriquecimiento.</p>
 *
 * @author Wara Pacheco
 * @version 3.1
 * @since 1.0.0
 */
@Slf4j
@Service
public class SearchSongsUseCase {

    private static final Executor ENRICHMENT_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Mínimo de resultados en BD para considerar la búsqueda satisfecha sin ir a APIs externas.
     * Por debajo de este umbral se fuerza un fetch externo aunque la BD no esté vacía.
     */
    private static final int MIN_RESULTS_THRESHOLD = 5;

    private final SongRepository songRepository;
    private final MusicProvider musicProvider;
    private final MusicAggregationService musicAggregationService;

    public SearchSongsUseCase(SongRepository songRepository, MusicProvider musicProvider,
                               MusicAggregationService musicAggregationService) {
        this.songRepository = songRepository;
        this.musicProvider = musicProvider;
        this.musicAggregationService = musicAggregationService;
    }

    public Page<Song> execute(SearchParams params) {
        Page<Song> result = songRepository.search(params);

        if (result.items().size() >= MIN_RESULTS_THRESHOLD) {
            // BD tiene suficientes resultados — devolver inmediatamente.
            // Si la página está incompleta, lanzar refresco en background para enriquecer
            // el corpus y que la próxima búsqueda tenga más variedad.
            if (params.page() == 0
                    && result.items().size() < params.pageSize()
                    && hasActiveQuadrant(params)) {
                CompletableFuture.runAsync(() -> backgroundRefresh(params), ENRICHMENT_EXECUTOR);
            }
            return result;
        }

        if (hasActiveQuadrant(params)) {
            List<Song> external = fetchFromExternalApis(params);

            List<CompletableFuture<Song>> futures = external.stream()
                    .map(song -> CompletableFuture.supplyAsync(
                            () -> musicAggregationService.enrich(song), ENRICHMENT_EXECUTOR))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            futures.stream()
                    .map(CompletableFuture::join)
                    .peek(song -> ensureSearchTags(song, params))
                    .forEach(song -> {
                        try {
                            songRepository.save(song);
                        } catch (Exception e) {
                            log.warn("No se pudo guardar '{}': {}", song.getTitle(), e.getMessage());
                        }
                    });

            result = songRepository.search(params);
        }

        return result;
    }

    /**
     * Fetch y guardado en background, sin bloquear la respuesta al cliente.
     * Se usa cuando la BD ya tiene resultados suficientes pero incompletos,
     * para ir poblando el corpus de cara a búsquedas futuras.
     */
    private void backgroundRefresh(SearchParams params) {
        try {
            List<Song> external = fetchFromExternalApis(params);
            external.stream()
                    .map(musicAggregationService::enrich)
                    .peek(song -> ensureSearchTags(song, params))
                    .forEach(song -> {
                        try {
                            songRepository.save(song);
                        } catch (Exception e) {
                            log.warn("Background save falló para '{}': {}", song.getTitle(), e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("Background refresh falló para params={}: {}", params, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    /** Devuelve true si al menos un cuadrante activo puede disparar búsqueda externa. */
    private boolean hasActiveQuadrant(SearchParams params) {
        return (params.q() != null && !params.q().isBlank())
                || (params.artist() != null && !params.artist().isBlank())
                || (params.aesthetic() != null && !params.aesthetic().isBlank())
                || (params.genre() != null && !params.genre().isBlank())
                || (params.decade() != null && !params.decade().isBlank())
                || (params.movie() != null && !params.movie().isBlank())
                || (params.country() != null && !params.country().isBlank())
                || (params.texture() != null && !params.texture().isBlank());
    }

    /**
     * Fetch con jerarquía: solo una fuente trae canciones, el resto son filtros.
     * movie → country → aesthetic → genre → texture → artist/q/decade
     */
    private List<Song> fetchFromExternalApis(SearchParams params) {
        int limit = params.pageSize();

        boolean hasMovie     = params.movie()     != null && !params.movie().isBlank();
        boolean hasCountry   = params.country()   != null && !params.country().isBlank();
        boolean hasArtist    = params.artist()    != null && !params.artist().isBlank();
        boolean hasAesthetic = params.aesthetic() != null && !params.aesthetic().isBlank();
        boolean hasGenre     = params.genre()     != null && !params.genre().isBlank();
        boolean hasTexture   = params.texture()   != null && !params.texture().isBlank();
        boolean hasTextOrDecade = (params.q() != null && !params.q().isBlank())
                               || (params.decade() != null && !params.decade().isBlank());

        if (hasMovie) {
            return new ArrayList<>(musicProvider.getSoundtrackSongs(params.movie()));
        }

        if (hasCountry) {
            // Cuadrante geográfico: busca artistas de ese país en MusicBrainz,
            // luego obtiene sus top tracks de Spotify para popularlos en BD.
            // El artista.country queda grabado antes del save para que el filtro en BD funcione.
            String country = params.country().trim();
            List<String> artistNames = musicProvider.getArtistNamesByCountry(country, 5);
            if (!artistNames.isEmpty()) {
                List<Song> songs = new ArrayList<>();
                int perArtist = Math.max(limit / artistNames.size(), 5);
                for (String artistName : artistNames) {
                    List<Song> artistSongs = musicProvider.searchSongs("artist:" + artistName, perArtist);
                    // Inyectar country en los artistas que coincidan por nombre
                    artistSongs.forEach(song -> {
                        if (song.getArtists() != null) {
                            song.getArtists().stream()
                                    .filter(a -> artistName.equalsIgnoreCase(a.getName()))
                                    .forEach(a -> a.setCountry(country));
                        }
                    });
                    songs.addAll(artistSongs);
                }
                return songs;
            }
            // Fallback: si MusicBrainz no devuelve artistas, buscar por nombre de país directamente
            return new ArrayList<>(musicProvider.searchSongs(country, limit));
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

        if (hasGenre) {
            // Si hay artista activo, ir directamente a Spotify con query combinada:
            // Last.fm ignora el filtro de artista y devolvería canciones incorrectas.
            if (!hasArtist) {
                // Last.fm como fuente primaria para géneros puros: tag.getTopTracks evita el
                // rate limit de Spotify (genre: en batch dispara 429 en Development Mode).
                // searchSongsByTag ya está @Cacheable — llamadas repetidas son gratuitas.
                List<Song> lastFmSongs = new ArrayList<>(musicProvider.searchSongsByTag(params.genre(), limit));
                if (!lastFmSongs.isEmpty()) {
                    return lastFmSongs;
                }
            }
            // Con artista activo, o si Last.fm no tiene el tag: Spotify con query combinada
            String spotifyQuery = buildSpotifyQuery(params);
            if (!spotifyQuery.isBlank()) {
                return new ArrayList<>(musicProvider.searchSongs(spotifyQuery, limit));
            }
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

        if (hasArtist || hasTextOrDecade) {
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
        if (params.genre() != null && !params.genre().isBlank()) {
            addTagIfMissing(song, params.genre().toLowerCase().trim());
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

        if (params.artist() != null && !params.artist().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append("artist:").append(params.artist().trim());
        }

        if (params.genre() != null && !params.genre().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append("genre:").append(params.genre().trim());
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