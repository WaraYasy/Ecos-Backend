package es.wara.ecos.backend.application.service;

import es.wara.ecos.backend.application.usecase.search.SearchParams;
import es.wara.ecos.backend.domain.model.Artist;
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
 * Servicio de ingesta de canciones desde APIs externas hacia la base de datos.
 *
 * <p>Centraliza el pipeline <b>fetch → enrich → save</b> que antes estaba disperso
 * en {@code SearchSongsUseCase} y {@code DatabaseSeeder}. Ahora ambos delegan aquí.</p>
 *
 * <p>La jerarquía de fuentes es: {@code movie → country → aesthetic → genre → texture → q/artist/decade}.
 * Solo una fuente trae canciones por llamada; los demás cuadrantes actúan como filtros en BD.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Service
public class SongIngestionService {

    private static final Executor EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final MusicProvider musicProvider;
    private final MusicAggregationService musicAggregationService;
    private final SongRepository songRepository;

    public SongIngestionService(MusicProvider musicProvider,
                                MusicAggregationService musicAggregationService,
                                SongRepository songRepository) {
        this.musicProvider = musicProvider;
        this.musicAggregationService = musicAggregationService;
        this.songRepository = songRepository;
    }

    /**
     * Obtiene canciones de APIs externas según los parámetros, las enriquece en paralelo
     * y las persiste en BD.
     *
     * @param params parámetros de búsqueda que determinan la fuente y los tags a garantizar
     * @return número de canciones guardadas con éxito
     */
    public int ingest(SearchParams params) {
        List<Song> songs = fetchSongs(params);
        if (songs.isEmpty()) {
            log.info("  [ingest] Sin canciones de APIs externas para [{}]", summarize(params));
            return 0;
        }
        log.info("  [ingest] {} canciones obtenidas, enriqueciendo...", songs.size());

        List<CompletableFuture<Song>> futures = songs.stream()
                .map(song -> CompletableFuture.supplyAsync(
                        () -> musicAggregationService.enrich(song), EXECUTOR))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        int saved = 0;
        for (CompletableFuture<Song> future : futures) {
            Song song = future.join();
            ensureSearchTags(song, params);
            try {
                songRepository.save(song);
                saved++;
            } catch (Exception e) {
                log.warn("  [ingest] No se pudo guardar '{}': {}", song.getTitle(), e.getMessage());
            }
        }
        log.info("  [ingest] {}/{} canciones guardadas en BD", saved, songs.size());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FETCH CON JERARQUÍA DE FUENTES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Selecciona la fuente externa según jerarquía y devuelve canciones sin enriquecer.
     * Jerarquía: movie → country → aesthetic → genre → texture → artist/q/decade
     */
    public List<Song> fetchSongs(SearchParams params) {
        int limit = params.pageSize();

        boolean hasMovie        = notBlank(params.movie());
        boolean hasCountry      = notBlank(params.country());
        boolean hasArtist       = notBlank(params.artist());
        boolean hasAesthetic    = notBlank(params.aesthetic());
        boolean hasGenre        = notBlank(params.genre());
        boolean hasTexture      = notBlank(params.texture());
        boolean hasTextOrDecade = notBlank(params.q()) || notBlank(params.decade());

        if (hasMovie) {
            log.info("  [fetch] movie='{}' → MusicBrainz + Spotify", params.movie());
            return new ArrayList<>(musicProvider.getSoundtrackSongs(params.movie()));
        }

        if (hasCountry) {
            String country = params.country().trim();
            List<String> artistNames = musicProvider.getArtistNamesByCountry(country, 5);
            if (!artistNames.isEmpty()) {
                log.info("  [fetch] country='{}' → {} artistas de MusicBrainz", country, artistNames.size());
                List<Song> songs = new ArrayList<>();
                int perArtist = Math.max(limit / artistNames.size(), 5);
                for (String artistName : artistNames) {
                    List<Song> artistSongs = musicProvider.searchSongs("artist:" + artistName, perArtist);
                    injectCountry(artistSongs, artistName, country);
                    songs.addAll(artistSongs);
                }
                return songs;
            }
            log.info("  [fetch] country='{}' → fallback Spotify", country);
            return new ArrayList<>(musicProvider.searchSongs(country, limit));
        }

        if (hasAesthetic) {
            if (hasTextOrDecade) {
                String q = buildSpotifyQuery(params);
                if (!q.isBlank()) {
                    log.info("  [fetch] aesthetic+decade/q → Spotify '{}'", q);
                    return new ArrayList<>(musicProvider.searchSongs(q, limit));
                }
            }
            log.info("  [fetch] aesthetic='{}' → Last.fm tag.getTopTracks", params.aesthetic());
            return new ArrayList<>(musicProvider.searchSongsByTag(params.aesthetic(), limit));
        }

        if (hasGenre) {
            if (!hasArtist) {
                List<Song> lastFmSongs = new ArrayList<>(musicProvider.searchSongsByTag(params.genre(), limit));
                if (!lastFmSongs.isEmpty()) {
                    log.info("  [fetch] genre='{}' → Last.fm tag.getTopTracks", params.genre());
                    return lastFmSongs;
                }
            }
            String q = buildSpotifyQuery(params);
            if (!q.isBlank()) {
                log.info("  [fetch] genre={} → Spotify '{}'",
                        hasArtist ? "genre+artist" : "genre (Last.fm vacío)", q);
                return new ArrayList<>(musicProvider.searchSongs(q, limit));
            }
        }

        if (hasTexture) {
            if (hasTextOrDecade) {
                String q = buildSpotifyQuery(params);
                if (!q.isBlank()) {
                    log.info("  [fetch] texture+decade/q → Spotify '{}'", q);
                    return new ArrayList<>(musicProvider.searchSongs(q, limit));
                }
            }
            log.info("  [fetch] texture='{}' → Last.fm tag.getTopTracks", params.texture());
            return new ArrayList<>(musicProvider.searchSongsByTag(params.texture(), limit));
        }

        if (hasArtist || hasTextOrDecade) {
            String q = buildSpotifyQuery(params);
            if (!q.isBlank()) {
                log.info("  [fetch] q/artist/decade → Spotify '{}'", q);
                return new ArrayList<>(musicProvider.searchSongs(q, limit));
            }
        }

        return new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Garantiza que la canción tenga el tag del cuadrante por el que fue buscada. */
    public void ensureSearchTags(Song song, SearchParams params) {
        if (notBlank(params.aesthetic())) addTagIfMissing(song, params.aesthetic().toLowerCase().trim());
        if (notBlank(params.genre()))     addTagIfMissing(song, params.genre().toLowerCase().trim());
        if (notBlank(params.texture()))   addTagIfMissing(song, params.texture().toLowerCase().trim());
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
        if (notBlank(params.q()))      sb.append(params.q().trim());
        if (notBlank(params.artist())) { if (!sb.isEmpty()) sb.append(" "); sb.append("artist:").append(params.artist().trim()); }
        if (notBlank(params.genre()))  { if (!sb.isEmpty()) sb.append(" "); sb.append("genre:").append(params.genre().trim()); }
        // decade: siempre "1980s" (validado en controller) → year:1980-1989
        if (notBlank(params.decade())) {
            int start = Integer.parseInt(params.decade().replace("s", "").trim());
            if (!sb.isEmpty()) sb.append(" ");
            sb.append("year:").append(start).append("-").append(start + 9);
        }
        // year: "1985" → year:1985-1985  |  "1980-1990" → year:1980-1990
        if (notBlank(params.year())) {
            String y = params.year().trim();
            if (!sb.isEmpty()) sb.append(" ");
            if (y.contains("-")) {
                sb.append("year:").append(y);
            } else {
                sb.append("year:").append(y).append("-").append(y);
            }
        }
        return sb.toString();
    }

    private void injectCountry(List<Song> songs, String artistName, String country) {
        songs.forEach(song -> {
            if (song.getArtists() != null) {
                song.getArtists().stream()
                        .filter(a -> artistName.equalsIgnoreCase(a.getName()))
                        .forEach(a -> a.setCountry(country));
            }
        });
    }

    public String summarize(SearchParams params) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(params.q()))         sb.append("q=").append(params.q().trim()).append(" ");
        if (notBlank(params.artist()))    sb.append("artist=").append(params.artist().trim()).append(" ");
        if (notBlank(params.aesthetic())) sb.append("aesthetic=").append(params.aesthetic().trim()).append(" ");
        if (notBlank(params.genre()))     sb.append("genre=").append(params.genre().trim()).append(" ");
        if (notBlank(params.decade()))    sb.append("decade=").append(params.decade().trim()).append(" ");
        if (notBlank(params.year()))      sb.append("year=").append(params.year().trim()).append(" ");
        if (notBlank(params.movie()))     sb.append("movie=").append(params.movie().trim()).append(" ");
        if (notBlank(params.country()))   sb.append("country=").append(params.country().trim()).append(" ");
        if (notBlank(params.texture()))   sb.append("texture=").append(params.texture().trim()).append(" ");
        return sb.isEmpty() ? "(sin cuadrantes)" : sb.toString().trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}