package es.wara.ecos.backend.infrastructure.seed;

import es.wara.ecos.backend.infrastructure.persistence.entity.*;
import es.wara.ecos.backend.infrastructure.persistence.jpa.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
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
 * <p>Se invoca desde {@code POST /v1/admin/songs/import} pasando el CSV como upload.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Component
public class CsvImportRunner {

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

    /**
     * Importa canciones desde cualquier {@link Reader} (archivo o upload HTTP).
     *
     * @param reader   fuente CSV
     * @param rowLimit máximo de filas a importar (0 = sin límite)
     * @param source   nombre de la fuente para los logs
     * @return array [imported, skipped, errors]
     */
    public int[] importFromReader(Reader reader, int rowLimit, String source) {
        log.info("[CsvImport] Iniciando importación desde '{}'", source);
        long start = System.currentTimeMillis();

        List<CSVRecord> records = readCsvFromReader(reader, source);
        if (records.isEmpty()) return new int[]{0, 0, 0};

        if (rowLimit > 0 && records.size() > rowLimit) {
            log.info("[CsvImport] Límite activo: procesando solo {} de {} filas", rowLimit, records.size());
            records = records.subList(0, rowLimit);
        }

        Map<String, ArtistEntity> artistCache = new HashMap<>();
        Map<String, AlbumEntity>  albumCache  = new HashMap<>();
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
        return new int[]{imported, skipped, errors};
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BATCH
    // ─────────────────────────────────────────────────────────────────────────

    /** Procesa un batch: cada fila en su propia transacción para aislar fallos. Devuelve [imported, skipped, errors]. */
    private int[] processBatch(List<CSVRecord> batch,
                               Map<String, ArtistEntity> artistCache,
                               Map<String, AlbumEntity>  albumCache) {
        int[] result = {0, 0, 0};
        for (CSVRecord r : batch) {
            int[] row = processRow(r, artistCache, albumCache);
            result[0] += row[0];
            result[1] += row[1];
            result[2] += row[2];
        }
        return result;
    }

    /** Procesa una sola fila en su propia transacción. Un duplicate key no envenena las demás filas. */
    private int[] processRow(CSVRecord r,
                             Map<String, ArtistEntity> artistCache,
                             Map<String, AlbumEntity>  albumCache) {
        try {
            if ("True".equalsIgnoreCase(r.get("incomplete"))) return new int[]{0, 1, 0};

            String spotifyId = r.get("spotify_id");
            if (spotifyId == null || spotifyId.isBlank()) return new int[]{0, 1, 0};

            // Saltar si la canción ya existe (fuera de tx para no contaminar nada)
            if (songRepo.findBySpotifyId(spotifyId).isPresent()) return new int[]{0, 1, 0};

            tx.execute(status -> {
                ArtistEntity artist = resolveArtist(r, artistCache);
                AlbumEntity  album  = resolveAlbum(r, artist, albumCache);
                SongEntity   song   = createSong(r, album, artist);
                createAudioFeatures(r, song);
                createTag(r, song);
                return null;
            });
            return new int[]{1, 0, 0};
        } catch (Exception e) {
            log.warn("[CsvImport] Error en fila '{}': {}", safeGet(r, "spotify_id"), e.getMessage());
            return new int[]{0, 0, 1};
        }
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
                    return albumRepo.save(a);
                });

        // Actualizar cover_url si el álbum ya existía sin él
        String coverUrl = safeGet(r, "cover_url");
        if (coverUrl != null && album.getCoverUrl() == null) {
            album.setCoverUrl(coverUrl);
            albumRepo.save(album);
        }
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
        song.setPreviewUrl(safeGet(r, "preview_url"));
        // Solo asignar ISRC si no está ya en uso por otra canción
        String isrc = safeGet(r, "isrc");
        if (isrc != null && songRepo.findByIsrc(isrc).isEmpty()) {
            song.setIsrc(isrc);
        }
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

    private List<CSVRecord> readCsvFromReader(Reader reader, String source) {
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(reader)) {

            List<CSVRecord> records = parser.getRecords();
            log.info("[CsvImport] {} filas leídas de '{}'", records.size(), source);
            return records;
        } catch (Exception e) {
            log.error("[CsvImport] No se pudo parsear el CSV '{}': {}", source, e.getMessage());
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