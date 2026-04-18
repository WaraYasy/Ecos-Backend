package es.wara.ecos.backend.infrastructure.scheduler;

import es.wara.ecos.backend.infrastructure.persistence.entity.AlbumEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.ArtistEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaAlbumRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaArtistRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pipeline de limpieza de datos del catálogo musical.
 *
 * <p>Se ejecuta cada noche (por defecto a las 03:00) y repara el estado
 * inconsistente que puede acumularse durante la importación de datos:</p>
 * <ol>
 *   <li>Rellena {@code artist_id} en álbumes huérfanos (Deezer + heurística).</li>
 *   <li>Vincula canciones sin álbum a uno existente o crea uno nuevo (iTunes).</li>
 *   <li>Elimina álbumes que quedaron vacíos tras la vinculación.</li>
 *   <li>Fusiona canciones duplicadas por título + artista.</li>
 *   <li>Rellena {@code preview_url} y {@code cover_url} vacíos vía iTunes.</li>
 * </ol>
 * <p>Al finalizar escribe un informe de texto en {@code /data/cleanup-YYYY-MM-DD.log}.</p>
 *
 * @author Wara Pacheco
 * @version 1.2
 * @since 1.0.0
 */
@Slf4j
@Component
public class DbCleanupScheduler {

    private static final String ITUNES_SEARCH = "https://itunes.apple.com/search";
    private static final String DEEZER_ALBUM  = "https://api.deezer.com/search/album";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JdbcTemplate        jdbc;
    private final JpaAlbumRepository  albumRepo;
    private final JpaSongRepository   songRepo;
    private final JpaArtistRepository artistRepo;
    private final RestTemplate        restTemplate;

    @Value("${cleanup.scheduler.itunes.delay-ms:400}")
    private long itunesDelayMs;

    @Value("${cleanup.log.dir:/data}")
    private String logDir;

    public DbCleanupScheduler(JdbcTemplate jdbc,
                               JpaAlbumRepository albumRepo,
                               JpaSongRepository songRepo,
                               JpaArtistRepository artistRepo,
                               RestTemplate restTemplate) {
        this.jdbc         = jdbc;
        this.albumRepo    = albumRepo;
        this.songRepo     = songRepo;
        this.artistRepo   = artistRepo;
        this.restTemplate = restTemplate;
    }

    // ── Informe de fase ───────────────────────────────────────────────────────

    private record PhaseResult(String name, boolean ok, String summary, String error) {
        static PhaseResult success(String name, String summary) { return new PhaseResult(name, true,  summary, null); }
        static PhaseResult failure(String name, String error)   { return new PhaseResult(name, false, null,    error); }
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    @Scheduled(cron = "${cleanup.scheduler.cron:0 0 3 * * *}")
    public void runCleanup() {
        log.info("[Cleanup] === Iniciando pipeline de limpieza ===");
        LocalDateTime start = LocalDateTime.now();
        List<PhaseResult> results = new ArrayList<>();

        results.add(runPhase("1 · Artistas de álbumes",   this::fixAlbumArtists));
        results.add(runPhase("2 · Canciones huérfanas",   this::linkOrphanSongs));
        results.add(runPhase("3 · Álbumes vacíos",        this::removeEmptyAlbums));
        results.add(runPhase("4 · Duplicados",            this::deduplicateSongs));
        results.add(runPhase("5 · Media (preview/cover)", this::fillMedia));

        log.info("[Cleanup] === Pipeline completado ===");
        writeReport(start, results);
    }

    private PhaseResult runPhase(String name, PhaseSupplier fn) {
        try {
            String summary = fn.run();
            return PhaseResult.success(name, summary);
        } catch (Exception e) {
            log.error("[Cleanup] Fase '{}' fallida", name, e);
            return PhaseResult.failure(name, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface PhaseSupplier { String run(); }

    // ── Fase 1: Fijar artistas de álbumes ─────────────────────────────────────

    @Transactional
    public String fixAlbumArtists() {
        List<AlbumEntity> albums = albumRepo.findByArtistIsNull();
        log.info("[Cleanup:1] {} álbumes sin artista", albums.size());
        int fixed = 0;

        for (AlbumEntity album : albums) {
            ArtistEntity artist = resolveAlbumArtist(album);
            if (artist != null) {
                album.setArtist(artist);
                albumRepo.save(album);
                fixed++;
                log.debug("[Cleanup:1] '{}' → {}", album.getTitle(), artist.getName());
            }
        }

        int linked = jdbc.update("""
            INSERT INTO song_artists (song_id, artist_id)
            SELECT s.id, a.artist_id
            FROM songs s
            JOIN albums a ON s.album_id = a.id
            LEFT JOIN song_artists sa ON sa.song_id = s.id
            WHERE sa.song_id IS NULL AND a.artist_id IS NOT NULL
            ON CONFLICT DO NOTHING
        """);

        String summary = "Álbumes corregidos: %d | Canciones vinculadas al artista: %d".formatted(fixed, linked);
        log.info("[Cleanup:1] {}", summary);
        return summary;
    }

    private ArtistEntity resolveAlbumArtist(AlbumEntity album) {
        String artistName = deezerAlbumArtist(album.getTitle());

        if (artistName == null) {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT ar.id FROM songs s
                JOIN song_artists sa ON sa.song_id = s.id
                JOIN artists ar ON ar.id = sa.artist_id
                WHERE s.album_id = ? LIMIT 1
            """, album.getId());
            if (!rows.isEmpty()) {
                return artistRepo.findById(UUID.fromString(rows.get(0).get("id").toString()))
                        .orElse(null);
            }
            return null;
        }
        return findOrCreateArtist(artistName);
    }

    // ── Fase 2: Vincular canciones sin álbum ──────────────────────────────────

    @Transactional
    public String linkOrphanSongs() {
        List<SongEntity> orphans = songRepo.findWithoutAlbum();
        log.info("[Cleanup:2] {} canciones sin álbum", orphans.size());
        int linked = 0; int created = 0;

        for (SongEntity song : orphans) {
            String artist = song.getArtists().isEmpty() ? "" : song.getArtists().get(0).getName();
            AlbumEntity album = resolveAlbumForSong(song.getTitle(), artist);
            if (album != null) {
                boolean isNew = album.getId() == null;
                song.setAlbum(album);
                songRepo.save(song);
                linked++;
                if (isNew) created++;
                log.debug("[Cleanup:2] '{}' → álbum '{}'", song.getTitle(), album.getTitle());
            }
        }
        String summary = "Canciones vinculadas: %d | Álbumes creados: %d".formatted(linked, created);
        log.info("[Cleanup:2] {}", summary);
        return summary;
    }

    private AlbumEntity resolveAlbumForSong(String songTitle, String artistName) {
        if (!artistName.isBlank()) {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT DISTINCT a.id FROM albums a
                JOIN songs s2 ON s2.album_id = a.id
                JOIN song_artists sa ON sa.song_id = s2.id
                JOIN artists ar ON ar.id = sa.artist_id
                WHERE lower(ar.name) = lower(?) LIMIT 1
            """, artistName);
            if (!rows.isEmpty()) {
                return albumRepo.findById(UUID.fromString(rows.get(0).get("id").toString())).orElse(null);
            }
        }

        Map<String, Object> hit = itunesSongSearch(songTitle, artistName);
        sleepQuietly(itunesDelayMs);
        if (hit != null) {
            String albumTitle = (String) hit.get("collectionName");
            if (albumTitle != null) {
                Optional<AlbumEntity> existing = albumRepo.findFirstByTitleIgnoreCase(albumTitle);
                if (existing.isPresent()) return existing.get();
                AlbumEntity album = new AlbumEntity();
                album.setTitle(albumTitle);
                album.setCoverUrl(hdCover((String) hit.get("artworkUrl100")));
                album.setYear(extractYear((String) hit.get("releaseDate")));
                album.setArtist(artistName.isBlank() ? null : findOrCreateArtist(artistName));
                return albumRepo.save(album);
            }
        }

        String generic = artistName.isBlank() ? "Unknown Album" : "Unknown Album — " + artistName;
        return albumRepo.findFirstByTitleIgnoreCase(generic).orElseGet(() -> {
            AlbumEntity album = new AlbumEntity();
            album.setTitle(generic);
            album.setArtist(artistName.isBlank() ? null : findOrCreateArtist(artistName));
            return albumRepo.save(album);
        });
    }

    // ── Fase 3: Eliminar álbumes vacíos ───────────────────────────────────────

    @Transactional
    public String removeEmptyAlbums() {
        List<AlbumEntity> empty = albumRepo.findEmpty();
        log.info("[Cleanup:3] {} álbumes vacíos", empty.size());
        albumRepo.deleteAll(empty);
        String summary = "Álbumes eliminados: %d".formatted(empty.size());
        log.info("[Cleanup:3] {}", summary);
        return summary;
    }

    // ── Fase 4: Deduplicar canciones ──────────────────────────────────────────

    @Transactional
    public String deduplicateSongs() {
        // Usamos una subquery para obtener UN solo artista por canción (el menor UUID).
        // Sin esto, un LEFT JOIN song_artists haría que una canción con N artistas apareciera
        // N veces y se agrupara en N grupos distintos, nunca detectándose como duplicada.
        List<Map<String, Object>> groups = jdbc.queryForList("""
            SELECT lower(s.title) AS norm_title,
                   (SELECT sa2.artist_id FROM song_artists sa2
                    WHERE sa2.song_id = s.id ORDER BY sa2.artist_id LIMIT 1) AS primary_artist_id,
                   array_agg(s.id ORDER BY
                       (s.preview_url IS NOT NULL)::int DESC,
                       (s.album_id    IS NOT NULL)::int DESC,
                       (s.spotify_id  IS NOT NULL)::int DESC,
                       s.created_at   ASC
                   ) AS ids
            FROM songs s
            GROUP BY lower(s.title),
                     (SELECT sa2.artist_id FROM song_artists sa2
                      WHERE sa2.song_id = s.id ORDER BY sa2.artist_id LIMIT 1)
            HAVING count(*) > 1
        """);

        log.info("[Cleanup:4] {} grupos duplicados", groups.size());
        int removed = 0;

        for (Map<String, Object> group : groups) {
            Object rawIds = group.get("ids");
            if (rawIds == null) continue;
            UUID[] ids = parseUuidArray(rawIds.toString());
            if (ids.length < 2) continue;
            UUID keepId = ids[0];
            for (int i = 1; i < ids.length; i++) {
                mergeDuplicate(keepId, ids[i]);
                removed++;
            }
        }
        String summary = "Grupos fusionados: %d | Canciones eliminadas: %d".formatted(groups.size(), removed);
        log.info("[Cleanup:4] {}", summary);
        return summary;
    }

    private void mergeDuplicate(UUID keepId, UUID dupId) {
        jdbc.update("""
            UPDATE playlist_songs SET song_id = ?
            WHERE song_id = ? AND NOT EXISTS (
                SELECT 1 FROM playlist_songs ps2
                WHERE ps2.song_id = ? AND ps2.playlist_id = playlist_songs.playlist_id
            )
        """, keepId, dupId, keepId);
        jdbc.update("DELETE FROM playlist_songs WHERE song_id = ?", dupId);

        jdbc.update("INSERT INTO song_artists (song_id, artist_id) SELECT ?, artist_id FROM song_artists WHERE song_id = ? ON CONFLICT DO NOTHING", keepId, dupId);
        jdbc.update("DELETE FROM song_artists WHERE song_id = ?", dupId);

        jdbc.update("INSERT INTO song_tags (song_id, tag, weight) SELECT ?, tag, weight FROM song_tags WHERE song_id = ? ON CONFLICT (song_id, tag) DO NOTHING", keepId, dupId);
        jdbc.update("DELETE FROM song_tags WHERE song_id = ?", dupId);

        jdbc.update("DELETE FROM audio_features    WHERE song_id = ?", dupId);
        jdbc.update("DELETE FROM song_embeddings   WHERE song_id = ?", dupId);
        jdbc.update("DELETE FROM song_movies        WHERE song_id = ?", dupId);
        jdbc.update("DELETE FROM user_interactions  WHERE song_id = ?", dupId);

        jdbc.update("""
            UPDATE songs SET
                preview_url = COALESCE(preview_url, (SELECT preview_url FROM songs WHERE id = ?)),
                album_id    = COALESCE(album_id,    (SELECT album_id    FROM songs WHERE id = ?)),
                spotify_id  = COALESCE(spotify_id,  (SELECT spotify_id  FROM songs WHERE id = ?)),
                isrc        = COALESCE(isrc,         (SELECT isrc        FROM songs WHERE id = ?))
            WHERE id = ?
        """, dupId, dupId, dupId, dupId, keepId);

        jdbc.update("DELETE FROM songs WHERE id = ?", dupId);
        log.debug("[Cleanup:4] fusionado {} → {}", dupId, keepId);
    }

    // ── Fase 5: Rellenar preview y cover via iTunes ───────────────────────────

    private record MediaRow(UUID songId, String title, String artistName,
                            UUID albumId, String albumCover, String previewUrl) {}

    @Transactional
    public String fillMedia() {
        // DISTINCT ON (s.id) evita filas duplicadas cuando una canción tiene varios artistas.
        // LEFT JOIN en lugar de subquery correlacionada: más eficiente con índices.
        List<MediaRow> rows = jdbc.query("""
            SELECT DISTINCT ON (s.id)
                   s.id, s.title, s.preview_url, s.album_id,
                   al.cover_url AS album_cover,
                   ar.name      AS artist_name
            FROM songs s
            LEFT JOIN albums      al ON al.id = s.album_id
            LEFT JOIN song_artists sa ON sa.song_id = s.id
            LEFT JOIN artists     ar ON ar.id = sa.artist_id
            WHERE s.preview_url IS NULL
               OR (s.album_id IS NOT NULL AND al.cover_url IS NULL)
            ORDER BY s.id, ar.name
        """, (rs, __) -> new MediaRow(
                UUID.fromString(rs.getString("id")),
                rs.getString("title"),
                rs.getString("artist_name"),
                rs.getString("album_id")  != null ? UUID.fromString(rs.getString("album_id"))  : null,
                rs.getString("album_cover"),
                rs.getString("preview_url")
        ));

        log.info("[Cleanup:5] {} canciones para enriquecer", rows.size());
        int fixedPreview = 0; int fixedCover = 0;

        for (MediaRow row : rows) {
            Map<String, Object> hit = itunesSongSearch(row.title(), row.artistName() != null ? row.artistName() : "");
            sleepQuietly(itunesDelayMs);
            if (hit == null) continue;

            String preview = (String) hit.get("previewUrl");
            String cover   = hdCover((String) hit.get("artworkUrl100"));

            if (preview != null && row.previewUrl() == null) {
                jdbc.update("UPDATE songs SET preview_url = ? WHERE id = ?", preview, row.songId());
                fixedPreview++;
            }
            if (cover != null && row.albumId() != null && (row.albumCover() == null || row.albumCover().isBlank())) {
                jdbc.update("UPDATE albums SET cover_url = ? WHERE id = ?", cover, row.albumId());
                fixedCover++;
            }
        }
        String summary = "Preview rellenados: %d | Covers rellenados: %d".formatted(fixedPreview, fixedCover);
        log.info("[Cleanup:5] {}", summary);
        return summary;
    }

    // ── Informe en fichero ────────────────────────────────────────────────────

    private void writeReport(LocalDateTime start, List<PhaseResult> results) {
        long errors = results.stream().filter(r -> !r.ok()).count();
        String status = errors > 0 ? "CON ERRORES (%d)".formatted(errors) : "OK";

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(60)).append("\n");
        sb.append("ECOS · Limpieza BD — %s — %s\n".formatted(start.format(DT_FMT), status));
        sb.append("=".repeat(60)).append("\n");
        for (PhaseResult r : results) {
            String mark = r.ok() ? "[OK]" : "[ERROR]";
            sb.append("  %s %s\n".formatted(mark, r.name()));
            if (r.ok())  sb.append("       %s\n".formatted(r.summary()));
            else         sb.append("       %s\n".formatted(r.error()));
        }
        sb.append("\n");

        try {
            Path dir  = Path.of(logDir);
            Files.createDirectories(dir);
            Path file = dir.resolve("cleanup-%s.log".formatted(LocalDate.now()));
            Files.writeString(file, sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("[Cleanup] Informe escrito en {}", file);
        } catch (IOException e) {
            log.error("[Cleanup] No se pudo escribir el informe en {}", logDir, e);
        }
    }

    // ── API helpers ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> itunesSongSearch(String title, String artist) {
        try {
            String url = UriComponentsBuilder.fromUriString(ITUNES_SEARCH)
                    .queryParam("term",   title + " " + artist)
                    .queryParam("media",  "music")
                    .queryParam("entity", "song")
                    .queryParam("limit",  1)
                    .toUriString();
            Map<?, ?> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) return null;
            List<?> r = (List<?>) resp.get("results");
            return (r != null && !r.isEmpty()) ? (Map<String, Object>) r.get(0) : null;
        } catch (Exception e) {
            log.debug("[Cleanup] iTunes error '{}': {}", title, e.getMessage());
            return null;
        }
    }

    private String deezerAlbumArtist(String albumTitle) {
        try {
            String url = UriComponentsBuilder.fromUriString(DEEZER_ALBUM)
                    .queryParam("q", albumTitle)
                    .queryParam("limit", 1)
                    .toUriString();
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) return null;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");
            if (data == null || data.isEmpty()) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> artist = (Map<String, Object>) data.get(0).get("artist");
            return artist != null ? (String) artist.get("name") : null;
        } catch (Exception e) {
            log.debug("[Cleanup] Deezer error '{}': {}", albumTitle, e.getMessage());
            return null;
        }
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private ArtistEntity findOrCreateArtist(String name) {
        return artistRepo.findByNameIgnoreCase(name).orElseGet(() -> {
            ArtistEntity a = new ArtistEntity();
            a.setName(name);
            return artistRepo.save(a);
        });
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private static String hdCover(String url) {
        return url != null ? url.replace("100x100bb", "600x600bb") : null;
    }

    private static Short extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        try { return Short.parseShort(releaseDate.substring(0, 4)); } catch (NumberFormatException e) { return null; }
    }

    private static UUID[] parseUuidArray(String raw) {
        String cleaned = raw.replaceAll("[{}]", "").trim();
        if (cleaned.isBlank()) return new UUID[0];
        String[] parts = cleaned.split(",");
        UUID[] uuids = new UUID[parts.length];
        for (int i = 0; i < parts.length; i++) uuids[i] = UUID.fromString(parts[i].trim());
        return uuids;
    }

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}