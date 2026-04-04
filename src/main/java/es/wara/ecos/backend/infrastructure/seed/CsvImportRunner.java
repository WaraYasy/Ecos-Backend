package es.wara.ecos.backend.infrastructure.seed;

import es.wara.ecos.backend.infrastructure.persistence.entity.*;
import es.wara.ecos.backend.infrastructure.persistence.jpa.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Importa el dataset de Spotify (CSV limpio) a la base de datos.
 *
 * <p>Solo se activa cuando {@code seed.csv.enabled=true}. Seguro ejecutar varias veces:
 * las canciones que ya existen por {@code spotify_id} se omiten.</p>
 *
 * <p>Qué persiste por cada fila del CSV:</p>
 * <ul>
 *   <li>{@code artists} — deduplicado por nombre (sin spotify_id aún)</li>
 *   <li>{@code albums} — deduplicado por {@code album_spotify_id}, con año</li>
 *   <li>{@code songs} — con {@code spotify_id}, título y duración</li>
 *   <li>{@code audio_features} — todas las métricas numéricas</li>
 *   <li>{@code song_artists} — relación canción-artista</li>
 *   <li>{@code song_tags} — tag inicial desde {@code genre_hint}</li>
 * </ul>
 *
 * <p>Campos que quedan pendientes para {@link EnrichmentMaintenanceJob}:</p>
 * <ul>
 *   <li>{@code artists.spotify_id}, {@code artists.country}</li>
 *   <li>{@code albums.cover_url}</li>
 *   <li>{@code songs.preview_url}</li>
 * </ul>
 *
 * <p>Para lanzar: {@code ./mvnw spring-boot:run -Dseed.csv.enabled=true}</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "seed.csv.enabled", havingValue = "true")
public class CsvImportRunner implements ApplicationRunner {

    @Value("${seed.csv.path:data/spotify_songs_clean.csv}")
    private String csvPath;

    /** 0 = sin límite (importa todo el CSV). */
    @Value("${seed.csv.limit:0}")
    private int limit;

    private static final int BATCH_SIZE = 200;

    private final JpaArtistRepository    artistRepo;
    private final JpaAlbumRepository     albumRepo;
    private final JpaSongRepository      songRepo;
    private final JpaAudioFeaturesRepository audioFeaturesRepo;
    private final JpaSongTagRepository   songTagRepo;
    private final TransactionTemplate    tx;

    public CsvImportRunner(JpaArtistRepository artistRepo,
                           JpaAlbumRepository albumRepo,
                           JpaSongRepository songRepo,
                           JpaAudioFeaturesRepository audioFeaturesRepo,
                           JpaSongTagRepository songTagRepo,
                           TransactionTemplate tx) {
        this.artistRepo       = artistRepo;
        this.albumRepo        = albumRepo;
        this.songRepo         = songRepo;
        this.audioFeaturesRepo = audioFeaturesRepo;
        this.songTagRepo      = songTagRepo;
        this.tx               = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("[CsvImport] Iniciando importación desde '{}'", csvPath);
        long start = System.currentTimeMillis();

        List<CSVRecord> records = readCsv();
        if (records.isEmpty()) return;

        if (limit > 0 && records.size() > limit) {
            log.info("[CsvImport] Límite activo: procesando solo {} de {} filas", limit, records.size());
            records = records.subList(0, limit);
        }

        // Cachés in-memory para evitar N+1 de lookups durante la importación
        Map<String, ArtistEntity> artistCache = new HashMap<>();
        Map<String, AlbumEntity>  albumCache  = new HashMap<>();

        // Pre-cargar los que ya existen en BD para que el caché empiece poblado
        preloadCaches(artistCache, albumCache);

        int total = records.size(), imported = 0, skipped = 0, errors = 0;
        List<CSVRecord> batch = new ArrayList<>(BATCH_SIZE);

        for (CSVRecord record : records) {
            batch.add(record);
            if (batch.size() >= BATCH_SIZE) {
                int[] result = processBatch(batch, artistCache, albumCache);
                imported += result[0];
                skipped  += result[1];
                errors   += result[2];
                batch.clear();
                log.info("[CsvImport] Progreso: {}/{} filas procesadas", imported + skipped + errors, total);
            }
        }
        if (!batch.isEmpty()) {
            int[] result = processBatch(batch, artistCache, albumCache);
            imported += result[0];
            skipped  += result[1];
            errors   += result[2];
        }

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        log.info("[CsvImport] Completado en {}s — {} importadas, {} ya existían, {} errores",
                elapsed, imported, skipped, errors);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BATCH
    // ─────────────────────────────────────────────────────────────────────────

    /** Procesa un batch dentro de una única transacción. Devuelve [imported, skipped, errors]. */
    private int[] processBatch(List<CSVRecord> batch,
                               Map<String, ArtistEntity> artistCache,
                               Map<String, AlbumEntity>  albumCache) {
        int[] result = {0, 0, 0};
        tx.execute(status -> {
            for (CSVRecord r : batch) {
                try {
                    if ("True".equalsIgnoreCase(r.get("incomplete"))) {
                        result[1]++;
                        continue;
                    }
                    String spotifyId = r.get("spotify_id");
                    if (spotifyId == null || spotifyId.isBlank()) { result[1]++; continue; }

                    // Saltar si la canción ya existe
                    if (songRepo.findBySpotifyId(spotifyId).isPresent()) { result[1]++; continue; }

                    ArtistEntity artist = resolveArtist(r, artistCache);
                    AlbumEntity  album  = resolveAlbum(r, artist, albumCache);
                    SongEntity   song   = createSong(r, album, artist);

                    createAudioFeatures(r, song);
                    createTag(r, song);

                    result[0]++;
                } catch (Exception e) {
                    log.warn("[CsvImport] Error en fila '{}': {}", safeGet(r, "spotify_id"), e.getMessage());
                    result[2]++;
                }
            }
            return null;
        });
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESOLVERS
    // ─────────────────────────────────────────────────────────────────────────

    private ArtistEntity resolveArtist(CSVRecord r, Map<String, ArtistEntity> cache) {
        String name = safeGet(r, "artist_name");
        if (name == null || name.isBlank()) return null;

        String key = name.toLowerCase();
        if (cache.containsKey(key)) return cache.get(key);

        ArtistEntity artist = artistRepo.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    ArtistEntity a = new ArtistEntity();
                    a.setName(name);
                    // spotify_id y country los llena el EnrichmentJob
                    return artistRepo.save(a);
                });
        cache.put(key, artist);
        return artist;
    }

    private AlbumEntity resolveAlbum(CSVRecord r, ArtistEntity artist, Map<String, AlbumEntity> cache) {
        String albumSpotifyId = safeGet(r, "album_spotify_id");
        if (albumSpotifyId == null || albumSpotifyId.isBlank()) return null;

        if (cache.containsKey(albumSpotifyId)) return cache.get(albumSpotifyId);

        AlbumEntity album = albumRepo.findBySpotifyId(albumSpotifyId)
                .orElseGet(() -> {
                    AlbumEntity a = new AlbumEntity();
                    a.setSpotifyId(albumSpotifyId);
                    a.setTitle(coalesce(safeGet(r, "album_title"), "Unknown Album"));
                    a.setYear(parseShort(safeGet(r, "release_year")));
                    a.setArtist(artist);
                    // cover_url la llena el EnrichmentJob
                    return albumRepo.save(a);
                });
        cache.put(albumSpotifyId, album);
        return album;
    }

    private SongEntity createSong(CSVRecord r, AlbumEntity album, ArtistEntity artist) {
        SongEntity song = new SongEntity();
        song.setSpotifyId(r.get("spotify_id"));
        song.setTitle(coalesce(safeGet(r, "title"), "Unknown Title"));
        song.setDurationMs(parseInt(safeGet(r, "duration_ms")));
        song.setAlbum(album);
        song.setCreatedAt(LocalDateTime.now());
        // preview_url la llena el EnrichmentJob (iTunes)
        songRepo.save(song);

        if (artist != null) {
            song.getArtists().add(artist);
            songRepo.save(song);
        }
        return song;
    }

    private void createAudioFeatures(CSVRecord r, SongEntity song) {
        AudioFeaturesEntity af = new AudioFeaturesEntity();
        af.setSong(song);
        af.setEnergy(parseBigDecimal(safeGet(r, "energy")));
        af.setTempo(parseBigDecimal(safeGet(r, "tempo")));
        af.setValence(parseBigDecimal(safeGet(r, "valence")));
        af.setDanceability(parseBigDecimal(safeGet(r, "danceability")));
        af.setAcousticness(parseBigDecimal(safeGet(r, "acousticness")));
        af.setInstrumentalness(parseBigDecimal(safeGet(r, "instrumentalness")));
        af.setSpeechiness(parseBigDecimal(safeGet(r, "speechiness")));
        af.setLoudness(parseBigDecimal(safeGet(r, "loudness")));
        af.setMusicalKey(parseShort(safeGet(r, "musical_key")));
        af.setMode(parseShort(safeGet(r, "mode")));
        // liveness está en el CSV pero no en el schema → ignorado
        // time_signature no está en el CSV → null
        audioFeaturesRepo.save(af);
    }

    private void createTag(CSVRecord r, SongEntity song) {
        String genre = safeGet(r, "genre_hint");
        if (genre == null || genre.isBlank()) return;

        SongTagId id  = new SongTagId(song.getId(), genre.toLowerCase().trim());
        SongTagEntity tag = new SongTagEntity();
        tag.setId(id);
        tag.setSong(song);
        tag.setWeight(BigDecimal.ONE);
        songTagRepo.save(tag);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INICIALIZACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    private void preloadCaches(Map<String, ArtistEntity> artistCache,
                               Map<String, AlbumEntity>  albumCache) {
        artistRepo.findAll().forEach(a -> {
            if (a.getName() != null) artistCache.put(a.getName().toLowerCase(), a);
        });
        albumRepo.findAll().forEach(a -> {
            if (a.getSpotifyId() != null) albumCache.put(a.getSpotifyId(), a);
        });
        log.info("[CsvImport] Caché precargada — {} artistas, {} álbumes ya en BD",
                artistCache.size(), albumCache.size());
    }

    private List<CSVRecord> readCsv() {
        try (Reader reader = new FileReader(csvPath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            List<CSVRecord> records = parser.getRecords();
            log.info("[CsvImport] {} filas leídas de '{}'", records.size(), csvPath);
            return records;
        } catch (Exception e) {
            log.error("[CsvImport] No se pudo leer el CSV '{}': {}", csvPath, e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS DE PARSEO
    // ─────────────────────────────────────────────────────────────────────────

    private String safeGet(CSVRecord r, String col) {
        try {
            String v = r.get(col);
            return (v == null || v.isBlank() || "nan".equalsIgnoreCase(v)) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String v) {
        if (v == null) return null;
        try { return new BigDecimal(v); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseInt(String v) {
        if (v == null) return null;
        try { return (int) Double.parseDouble(v); } catch (NumberFormatException e) { return null; }
    }

    private Short parseShort(String v) {
        if (v == null) return null;
        try { return (short) Double.parseDouble(v); } catch (NumberFormatException e) { return null; }
    }

    private String coalesce(String v, String fallback) {
        return (v != null && !v.isBlank()) ? v : fallback;
    }
}