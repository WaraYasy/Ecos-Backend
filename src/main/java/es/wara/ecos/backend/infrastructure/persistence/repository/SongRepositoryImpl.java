package es.wara.ecos.backend.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.wara.ecos.backend.application.usecase.search.SearchParams;
import es.wara.ecos.backend.domain.model.Movie;
import es.wara.ecos.backend.domain.model.Page;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.domain.port.SongRepository;
import es.wara.ecos.backend.infrastructure.persistence.entity.AlbumEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.ArtistEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.AudioFeaturesEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.MovieEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaAlbumRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaArtistRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaAudioFeaturesRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaMovieRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.AudioFeaturesMapper;
import es.wara.ecos.backend.infrastructure.persistence.mapper.SongMapper;
import es.wara.ecos.backend.infrastructure.persistence.specification.SongSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del puerto de dominio {@link SongRepository} usando JPA + JdbcTemplate.
 *
 * <p><b>Deduplicación en tres niveles en {@link #save(Song)}:</b>
 * <ol>
 *   <li>ISRC (grabación física): si ya existe, actualiza spotifyId, álbum y películas si
 *       cambiaron, y añade los tags nuevos que no estuvieran en BD.</li>
 *   <li>spotifyId (edición de Spotify): si ya existe, mismo comportamiento de actualización
 *       parcial + merge de tags.</li>
 *   <li>Nueva canción: inserta con todas sus relaciones (álbum, artistas, películas,
 *       audio features, tags).</li>
 * </ol>
 *
 * <p><b>Upserts thread-safe:</b> álbumes, artistas y películas se insertan con
 * {@code INSERT ... ON CONFLICT DO NOTHING} vía {@link JdbcTemplate}. Esto garantiza
 * que múltiples hilos concurrentes no provoquen {@code DataIntegrityViolationException}
 * al intentar insertar el mismo registro simultáneamente: la base de datos resuelve el
 * conflicto de forma atómica y el SELECT posterior siempre encuentra el registro.</p>
 *
 * <p><b>Búsqueda multidimensional:</b> {@link #search(SearchParams)} delega en
 * {@link SongSpecification#from(SearchParams)} para combinar con AND los predicados
 * de los 5 cuadrantes activos, con paginación.</p>
 *
 * @author Wara Pacheco
 * @version 1.3
 * @since 1.0.0
 */
@Slf4j
@Repository
public class SongRepositoryImpl implements SongRepository {

    private final JpaSongRepository jpa;
    private final JpaArtistRepository artistJpa;
    private final JpaAlbumRepository albumJpa;
    private final JpaMovieRepository movieJpa;
    private final JpaAudioFeaturesRepository audioFeaturesJpa;
    private final SongMapper mapper;
    private final AudioFeaturesMapper audioFeaturesMapper;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public SongRepositoryImpl(JpaSongRepository jpa,
                               JpaArtistRepository artistJpa,
                               JpaAlbumRepository albumJpa,
                               JpaMovieRepository movieJpa,
                               JpaAudioFeaturesRepository audioFeaturesJpa,
                               SongMapper mapper,
                               AudioFeaturesMapper audioFeaturesMapper,
                               JdbcTemplate jdbc,
                               ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.artistJpa = artistJpa;
        this.albumJpa = albumJpa;
        this.movieJpa = movieJpa;
        this.audioFeaturesJpa = audioFeaturesJpa;
        this.mapper = mapper;
        this.audioFeaturesMapper = audioFeaturesMapper;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LECTURA
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<Song> findById(UUID id) {
        return jpa.findById(id).map(mapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> findAllById(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jpa.findAllById(ids).stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Song> findBySpotifyId(String spotifyId) {
        return jpa.findBySpotifyId(spotifyId).map(mapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Song> findByIsrc(String isrc) {
        return jpa.findByIsrc(isrc).map(mapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> findByTitleContaining(String title) {
        return jpa.findByTitleContainingIgnoreCase(title).stream()
                .map(mapper::toModel)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ESCRITURA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persiste una canción aplicando deduplicación en tres niveles:
     * <ol>
     *   <li>Si el ISRC ya existe → actualizar campos faltantes y merge de tags.</li>
     *   <li>Si el spotifyId ya existe → ídem.</li>
     *   <li>Si no existe → insertar nueva con todas sus relaciones.</li>
     * </ol>
     * Los helper {@code upsertAlbum}, {@code upsertArtist} y {@code upsertMovie}
     * usan {@code ON CONFLICT DO NOTHING} para ser seguros ante concurrencia.
     */
    @Override
    @Transactional
    public Song save(Song song) {
        // Normalizar ISRC: eliminar guiones (US-RC1-76-07839 → USRC17607839)
        // para que siempre quede en formato compacto de 12 chars y no exceda VARCHAR(15)
        if (song.getIsrc() != null) {
            song.setIsrc(song.getIsrc().replace("-", ""));
        }

        // 1. Dedup por ISRC
        if (song.getIsrc() != null) {
            Optional<SongEntity> byIsrc = jpa.findByIsrc(song.getIsrc());
            if (byIsrc.isPresent()) {
                SongEntity existing = byIsrc.get();
                boolean updated = false;
                if (song.getSpotifyId() != null
                        && !song.getSpotifyId().equals(existing.getSpotifyId())) {
                    existing.setSpotifyId(song.getSpotifyId());
                    updated = true;
                }
                if (existing.getAlbum() == null && song.getAlbum() != null) {
                    AlbumEntity album = upsertAlbum(song.getAlbum());
                    if (album != null) { existing.setAlbum(album); updated = true; }
                }
                if (existing.getMovies().isEmpty() && song.getMovies() != null && !song.getMovies().isEmpty()) {
                    upsertMoviesToSong(existing, song.getMovies());
                    updated = true;
                }
                if (existing.getPreviewUrl() == null && song.getPreviewUrl() != null) {
                    existing.setPreviewUrl(song.getPreviewUrl());
                    updated = true;
                }
                if (updated) jpa.save(existing);
                addMissingTags(existing, song.getTags());
                return mapper.toModel(existing);
            }
        }

        // 2. Dedup por spotifyId
        if (song.getSpotifyId() != null) {
            Optional<SongEntity> bySpotifyId = jpa.findBySpotifyId(song.getSpotifyId());
            if (bySpotifyId.isPresent()) {
                SongEntity existing = bySpotifyId.get();
                boolean updated = false;
                if (existing.getAlbum() == null && song.getAlbum() != null) {
                    AlbumEntity album = upsertAlbum(song.getAlbum());
                    if (album != null) { existing.setAlbum(album); updated = true; }
                }
                if (existing.getMovies().isEmpty() && song.getMovies() != null && !song.getMovies().isEmpty()) {
                    upsertMoviesToSong(existing, song.getMovies());
                    updated = true;
                }
                if (existing.getPreviewUrl() == null && song.getPreviewUrl() != null) {
                    existing.setPreviewUrl(song.getPreviewUrl());
                    updated = true;
                }
                if (updated) jpa.save(existing);
                addMissingTags(existing, song.getTags());
                return mapper.toModel(existing);
            }
        }

        // 3. Nueva canción
        SongEntity entity = mapper.toEntity(song);

        if (song.getAlbum() != null) {
            entity.setAlbum(upsertAlbum(song.getAlbum()));
        }

        if (song.getArtists() != null) {
            for (es.wara.ecos.backend.domain.model.Artist artist : song.getArtists()) {
                if (artist.getName() == null || artist.getName().isBlank()) continue;
                ArtistEntity artistEntity = upsertArtist(artist);
                if (artistEntity != null) entity.getArtists().add(artistEntity);
            }
        }

        if (song.getMovies() != null) {
            for (Movie movie : song.getMovies()) {
                if (movie.getTitle() == null) continue;
                MovieEntity movieEntity = upsertMovie(movie);
                if (movieEntity != null) entity.getMovies().add(movieEntity);
            }
        }

        SongEntity saved;
        try {
            saved = jpa.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Race condition: otro hilo insertó la misma canción simultáneamente.
            // La canción SÍ está en BD — re-fetchear por spotifyId o ISRC y continuar.
            if (song.getSpotifyId() != null) {
                Optional<SongEntity> existing = jpa.findBySpotifyId(song.getSpotifyId());
                if (existing.isPresent()) {
                    addMissingTags(existing.get(), song.getTags());
                    return mapper.toModel(existing.get());
                }
            }
            if (song.getIsrc() != null) {
                Optional<SongEntity> existing = jpa.findByIsrc(song.getIsrc());
                if (existing.isPresent()) {
                    addMissingTags(existing.get(), song.getTags());
                    return mapper.toModel(existing.get());
                }
            }
            throw e;
        }

        if (song.getAudioFeatures() != null) {
            AudioFeaturesEntity af = audioFeaturesMapper.toEntity(song.getAudioFeatures());
            af.setSong(saved);
            audioFeaturesJpa.save(af);
        }

        addMissingTags(saved, song.getTags());

        return mapper.toModel(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BÚSQUEDA
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<Song> search(SearchParams params) {
        Specification<SongEntity> spec = SongSpecification.from(params);
        PageRequest pageRequest = PageRequest.of(params.page(), params.pageSize());
        org.springframework.data.domain.Page<SongEntity> result = jpa.findAll(spec, pageRequest);
        List<Song> items = new ArrayList<>(result.stream().map(mapper::toModel).toList());
        if (params.page() == 0) {
            Collections.shuffle(items);
        }
        return new Page<>(items, params.page(), params.pageSize(), result.getTotalElements());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPSERTS THREAD-SAFE (ON CONFLICT DO NOTHING)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inserta un álbum si no existe (por spotifyId o coverUrl) y devuelve la entidad.
     * Thread-safe: usa {@code ON CONFLICT DO NOTHING} — no lanza excepción si otro hilo
     * insertó el mismo registro simultáneamente.
     */
    private AlbumEntity upsertAlbum(es.wara.ecos.backend.domain.model.Album album) {
        if (album.getSpotifyId() != null) {
            jdbc.update(
                "INSERT INTO albums (id, spotify_id, title, year, cover_url) " +
                "VALUES (gen_random_uuid(), ?, ?, ?, ?) " +
                "ON CONFLICT (spotify_id) WHERE spotify_id IS NOT NULL DO NOTHING",
                album.getSpotifyId(),
                album.getTitle(),
                album.getYear() != null ? album.getYear().shortValue() : null,
                album.getCoverUrl()
            );
            return albumJpa.findBySpotifyId(album.getSpotifyId()).orElse(null);
        }
        if (album.getCoverUrl() != null) {
            jdbc.update(
                "INSERT INTO albums (id, title, cover_url) " +
                "VALUES (gen_random_uuid(), ?, ?) " +
                "ON CONFLICT (cover_url) WHERE cover_url IS NOT NULL DO NOTHING",
                album.getTitle(),
                album.getCoverUrl()
            );
            return albumJpa.findByCoverUrl(album.getCoverUrl()).orElse(null);
        }
        return null;
    }

    /**
     * Inserta un artista si no existe (por spotifyId o nombre) y devuelve la entidad.
     * Thread-safe: usa {@code ON CONFLICT DO NOTHING}.
     */
    private ArtistEntity upsertArtist(es.wara.ecos.backend.domain.model.Artist artist) {
        String country = artist.getCountry();

        if (artist.getSpotifyId() != null) {
            jdbc.update(
                "INSERT INTO artists (id, spotify_id, name, country) " +
                "VALUES (gen_random_uuid(), ?, ?, ?) " +
                "ON CONFLICT (spotify_id) WHERE spotify_id IS NOT NULL DO NOTHING",
                artist.getSpotifyId(), artist.getName(), country
            );
            // Si el artista ya existía sin country y ahora tenemos uno, lo actualizamos
            if (country != null) {
                jdbc.update(
                    "UPDATE artists SET country = ? WHERE spotify_id = ? AND country IS NULL",
                    country, artist.getSpotifyId()
                );
            }
            return artistJpa.findBySpotifyId(artist.getSpotifyId()).orElse(null);
        }
        // Artista sin spotifyId: dedup por nombre en minúsculas
        jdbc.update(
            "INSERT INTO artists (id, name, country) " +
            "VALUES (gen_random_uuid(), ?, ?) " +
            "ON CONFLICT (lower(name)) WHERE spotify_id IS NULL DO NOTHING",
            artist.getName(), country
        );
        if (country != null) {
            jdbc.update(
                "UPDATE artists SET country = ? WHERE lower(name) = lower(?) AND country IS NULL",
                country, artist.getName()
            );
        }
        return artistJpa.findByNameIgnoreCase(artist.getName()).orElse(null);
    }

    /**
     * Inserta una película o serie si no existe (por tmdbId + mediaType) y devuelve la entidad.
     * Si ya existe, completa director y géneros solo si estaban a NULL (nunca sobreescribe datos).
     * Thread-safe: usa {@code ON CONFLICT ... DO UPDATE} con {@code COALESCE}.
     */
    private MovieEntity upsertMovie(Movie movie) {
        if (movie.getTmdbId() == null || movie.getTitle() == null) return null;
        String mediaType = movie.getMediaType() != null ? movie.getMediaType() : "movie";
        jdbc.update(
            "INSERT INTO movies (id, tmdb_id, title, year, director, genres, media_type) " +
            "VALUES (gen_random_uuid(), ?, ?, ?, ?, CAST(? AS jsonb), ?) " +
            "ON CONFLICT (tmdb_id, media_type) DO UPDATE SET " +
            "  director = COALESCE(movies.director, EXCLUDED.director), " +
            "  genres   = COALESCE(movies.genres,   EXCLUDED.genres)",
            movie.getTmdbId(),
            movie.getTitle(),
            movie.getYear() != null ? movie.getYear().shortValue() : null,
            movie.getDirector(),
            toJsonb(movie.getGenres()),
            mediaType
        );
        return movieJpa.findByTmdbIdAndMediaType(movie.getTmdbId(), mediaType).orElse(null);
    }

    /** Asocia películas a una canción ya existente usando upserts thread-safe. */
    private void upsertMoviesToSong(SongEntity songEntity, List<Movie> movies) {
        for (Movie movie : movies) {
            MovieEntity movieEntity = upsertMovie(movie);
            if (movieEntity != null) songEntity.getMovies().add(movieEntity);
        }
    }

    /** Persiste los tags nuevos de una canción ya existente, ignorando los que ya tenga.
     *  Thread-safe: usa ON CONFLICT DO NOTHING para evitar race conditions entre hilos.
     *  Usa batchUpdate para reducir los round-trips a la BD a uno solo por canción. */
    private void addMissingTags(SongEntity song, List<Tag> tags) {
        if (tags == null || tags.isEmpty()) return;
        List<Object[]> batch = tags.stream()
                .filter(t -> t.getName() != null && !t.getName().isBlank())
                .map(t -> new Object[]{ song.getId(), t.getName(), t.getWeight() })
                .toList();
        if (batch.isEmpty()) return;
        jdbc.batchUpdate(
            "INSERT INTO song_tags (song_id, tag, weight) VALUES (?, ?, ?) ON CONFLICT (song_id, tag) DO NOTHING",
            batch
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILIDADES
    // ─────────────────────────────────────────────────────────────────────────

    /** Serializa una lista de strings a JSON para almacenar como JSONB en PostgreSQL. */
    private String toJsonb(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar genres a JSONB: {}", e.getMessage());
            return null;
        }
    }
}
