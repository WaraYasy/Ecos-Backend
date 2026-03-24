package es.wara.ecos.backend.application.service;

import es.wara.ecos.backend.domain.model.Movie;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.domain.port.MusicProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Servicio de aplicación que orquesta el enriquecimiento de canciones y la importación
 * de soundtracks combinando las cuatro fuentes externas de ECOS.
 *
 * <p>Los dos casos de uso principales son:
 * <ul>
 *   <li>{@link #enrich(Song)} — dado un {@link Song} básico (obtenido de Spotify),
 *       añade en paralelo audio features (Spotify), tags (Last.fm) y películas
 *       relacionadas (MusicBrainz + TMDb). Las tres llamadas se lanzan simultáneamente
 *       para minimizar la latencia total.</li>
 *   <li>{@link #importSoundtrack(String)} — dado un título de película, obtiene el
 *       tracklist completo del OST (MusicBrainz → Spotify) y enriquece cada pista.</li>
 * </ul>
 * </p>
 *
 * <p><b>Degradación elegante:</b> si una fuente falla o supera el timeout de 30 s, el
 * enriquecimiento continúa con los datos disponibles de las fuentes restantes.</p>
 *
 * <p><b>Rate limit MusicBrainz:</b> el {@code getMoviesForSong} del enriquecimiento llama
 * internamente a {@code MusicBrainzClient}, que aplica un throttle de 1.1 s/petición.
 * Al importar un soundtrack con N canciones, espera al menos N × 1.1 s por el paso
 * de MusicBrainz de cada pista.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@Service
public class MusicAggregationService {

    private static final int ENRICH_TIMEOUT_SECONDS = 30;

    private final MusicProvider musicProvider;

    public MusicAggregationService(MusicProvider musicProvider) {
        this.musicProvider = musicProvider;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API PÚBLICA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Enriquece un {@link Song} básico añadiendo audio features, tags y películas en paralelo.
     *
     * <p>Las tres llamadas se lanzan simultáneamente como {@link CompletableFuture}:
     * <ul>
     *   <li>Audio features → Spotify (requiere {@code spotifyId})</li>
     *   <li>Tags → Last.fm (requiere artista y título)</li>
     *   <li>Películas → MusicBrainz + TMDb (requiere artista y título)</li>
     * </ul>
     * Si alguna llamada falla se ignora su resultado y la canción se devuelve con
     * los datos que sí estén disponibles.
     * </p>
     *
     * @param song canción básica a enriquecer; se modifica in-place y se devuelve
     * @return la misma instancia de {@code song} con los campos añadidos
     */
    public Song enrich(Song song) {
        if (song == null) return null;

        String artist = extractPrimaryArtist(song);
        String title  = song.getTitle() != null ? song.getTitle() : "";

        log.debug("Enriqueciendo canción: '{}' - '{}'", artist, title);

        // Lanzar las fuentes en paralelo
        CompletableFuture<Void> tagsFuture = CompletableFuture.runAsync(() -> {
            if (!artist.isBlank() && !title.isBlank()) {
                try {
                    List<Tag> tags = musicProvider.getSongTags(artist, cleanTitle(title));
                    song.setTags(tags);
                } catch (Exception e) {
                    log.warn("No se pudieron obtener tags para '{} - {}': {}", artist, title, e.getMessage());
                }
            }
        });

        CompletableFuture<Void> moviesFuture = CompletableFuture.runAsync(() -> {
            if (!artist.isBlank() && !title.isBlank()
                    && (song.getMovies() == null || song.getMovies().isEmpty())) {
                try {
                    List<Movie> movies = musicProvider.getMoviesForSong(artist, title);
                    song.setMovies(movies);
                } catch (Exception e) {
                    log.warn("No se pudieron obtener películas para '{} - {}': {}", artist, title, e.getMessage());
                }
            }
        });

        // Esperar todas las fuentes con timeout global
        try {
            CompletableFuture.allOf(tagsFuture, moviesFuture)
                    .get(ENRICH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Timeout al enriquecer '{}': alguna fuente no respondió en {}s", title, ENRICH_TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.warn("Error al esperar enriquecimiento de '{}': {}", title, e.getMessage());
        }

        log.debug("Enriquecimiento completado para '{}': tags={}, movies={}",
                title,
                song.getTags() != null ? song.getTags().size() : 0,
                song.getMovies() != null ? song.getMovies().size() : 0);

        return song;
    }

    /**
     * Obtiene e importa todas las canciones del soundtrack de una película, ya enriquecidas.
     *
     * <p>Flujo: MusicBrainz (tracklist OST) → Spotify (enriquecer) → {@link #enrich(Song)}
     * para cada pista. Las canciones se enriquecen secuencialmente para respetar el
     * rate limit de MusicBrainz (1 req/s) en el paso {@code getMoviesForSong} de cada pista.
     * </p>
     *
     * @param movieTitle título de la película cuyo OST se quiere importar
     * @return lista de canciones del soundtrack enriquecidas; vacía si no se encontró OST
     */
    public List<Song> importSoundtrack(String movieTitle) {
        log.info("Importando soundtrack de '{}'", movieTitle);

        List<Song> songs = musicProvider.getSoundtrackSongs(movieTitle);

        if (songs.isEmpty()) {
            log.info("No se encontró OST para '{}'", movieTitle);
            return List.of();
        }

        log.info("Enriqueciendo {} pistas del soundtrack de '{}'", songs.size(), movieTitle);

        // Enriquecimiento secuencial para respetar el throttle de MusicBrainzClient
        return songs.stream().map(this::enrich).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos privados
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Elimina sufijos de remasterización/edición que Last.fm no reconoce.
     * Ejemplos: "Dreams - 2004 Remaster" → "Dreams", "Don't Stop Me Now - Remastered 2011" → "Don't Stop Me Now"
     */
    private static String cleanTitle(String title) {
        return title
                .replaceAll("(?i)\\s*[-–]\\s*(Remastered|Remaster)(\\s+\\d{4})?.*$", "")
                .replaceAll("(?i)\\s*[-–]\\s*\\d{4}\\s+(Remastered|Remaster).*$", "")
                .replaceAll("(?i)\\s*\\((Remastered|Remaster)[^)]*\\)", "")
                .replaceAll("(?i)\\s*\\((Live|Radio Edit|Single Version|Album Version)[^)]*\\)", "")
                .replaceAll("(?i)\\s*-\\s*(Live|Radio Edit|Single Version|Album Version)\\s*$", "")
                .trim();
    }

    /** Extrae el nombre del artista principal de la canción, o cadena vacía si no hay. */
    private String extractPrimaryArtist(Song song) {
        if (song.getArtists() != null && !song.getArtists().isEmpty()) {
            String name = song.getArtists().get(0).getName();
            return name != null ? name : "";
        }
        return "";
    }
}