package es.wara.ecos.backend.infrastructure.persistence.repository;

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
import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagId;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaAlbumRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaArtistRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaAudioFeaturesRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaMovieRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongTagRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.AudioFeaturesMapper;
import es.wara.ecos.backend.infrastructure.persistence.mapper.SongMapper;
import es.wara.ecos.backend.infrastructure.persistence.specification.SongSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del puerto de dominio {@link SongRepository} usando JPA.
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
 * El merge de tags ({@link #addMissingTags}) garantiza que los tags asignados por
 * {@code SearchSongsUseCase.ensureSearchTags} se persistan incluso cuando la canción
 * ya existía en BD sin ese tag.</p>
 *
 * <p><b>Búsqueda multidimensional:</b> {@link #search(SearchParams)} delega en
 * {@link SongSpecification#from(SearchParams)} para combinar con AND los predicados
 * de los 5 cuadrantes activos, con paginación.</p>
 *
 * @author Wara Pacheco
 * @version 1.2
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
    private final JpaSongTagRepository songTagJpa;
    private final SongMapper mapper;
    private final AudioFeaturesMapper audioFeaturesMapper;

    public SongRepositoryImpl(JpaSongRepository jpa, JpaArtistRepository artistJpa,
                               JpaAlbumRepository albumJpa, JpaMovieRepository movieJpa,
                               JpaAudioFeaturesRepository audioFeaturesJpa,
                               JpaSongTagRepository songTagJpa,
                               SongMapper mapper, AudioFeaturesMapper audioFeaturesMapper) {
        this.jpa = jpa;
        this.artistJpa = artistJpa;
        this.albumJpa = albumJpa;
        this.movieJpa = movieJpa;
        this.audioFeaturesJpa = audioFeaturesJpa;
        this.songTagJpa = songTagJpa;
        this.mapper = mapper;
        this.audioFeaturesMapper = audioFeaturesMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Song> findById(UUID id) {
        return jpa.findById(id).map(mapper::toModel);
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

    /**
     * Persiste una canción aplicando deduplicación en tres niveles:
     * <ol>
     *   <li>Si el ISRC es conocido y ya existe en BD → devolver la existente
     *       (actualizar spotifyId si la grabación cambió de ID en Spotify).</li>
     *   <li>Si el spotifyId ya existe en BD → devolver la existente (idempotente).</li>
     *   <li>Si no existe → insertar nueva canción.</li>
     * </ol>
     */
    @Override
    @Transactional
    public Song save(Song song) {
        // 1. Dedup por ISRC (grabación física)
        if (song.getIsrc() != null) {
            Optional<SongEntity> byIsrc = jpa.findByIsrc(song.getIsrc());
            if (byIsrc.isPresent()) {
                SongEntity existing = byIsrc.get();
                boolean updated = false;
                if (song.getSpotifyId() != null
                        && !song.getSpotifyId().equals(existing.getSpotifyId())) {
                    log.info("ISRC {} ya existe con spotifyId={}, actualizando a {}",
                            song.getIsrc(), existing.getSpotifyId(), song.getSpotifyId());
                    existing.setSpotifyId(song.getSpotifyId());
                    updated = true;
                }
                if (existing.getAlbum() == null && song.getAlbum() != null
                        && song.getAlbum().getSpotifyId() != null) {
                    existing.setAlbum(findOrCreateAlbum(song.getAlbum()));
                    updated = true;
                }
                if (existing.getMovies().isEmpty() && song.getMovies() != null && !song.getMovies().isEmpty()) {
                    addMoviesToSong(existing, song.getMovies());
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

        // 2. Dedup por spotifyId (misma edición de Spotify)
        if (song.getSpotifyId() != null) {
            Optional<SongEntity> bySpotifyId = jpa.findBySpotifyId(song.getSpotifyId());
            if (bySpotifyId.isPresent()) {
                SongEntity existing = bySpotifyId.get();
                boolean updated = false;
                if (existing.getAlbum() == null && song.getAlbum() != null
                        && song.getAlbum().getSpotifyId() != null) {
                    existing.setAlbum(findOrCreateAlbum(song.getAlbum()));
                    updated = true;
                }
                if (existing.getMovies().isEmpty() && song.getMovies() != null && !song.getMovies().isEmpty()) {
                    addMoviesToSong(existing, song.getMovies());
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

        // 3. Nueva canción — insertar con todas sus relaciones
        SongEntity entity = mapper.toEntity(song);

        // Álbum: find-or-create por spotifyId
        if (song.getAlbum() != null && song.getAlbum().getSpotifyId() != null) {
            entity.setAlbum(findOrCreateAlbum(song.getAlbum()));
        }

        // Artistas: find-or-create por spotifyId
        if (song.getArtists() != null) {
            for (es.wara.ecos.backend.domain.model.Artist artist : song.getArtists()) {
                if (artist.getSpotifyId() == null) continue;
                ArtistEntity artistEntity = artistJpa.findBySpotifyId(artist.getSpotifyId())
                        .orElseGet(() -> {
                            ArtistEntity a = new ArtistEntity();
                            a.setSpotifyId(artist.getSpotifyId());
                            a.setName(artist.getName());
                            return artistJpa.save(a);
                        });
                entity.getArtists().add(artistEntity);
            }
        }

        // Películas: find-or-create por tmdbId
        if (song.getMovies() != null) {
            for (Movie movie : song.getMovies()) {
                if (movie.getTitle() == null) continue;
                MovieEntity movieEntity = movieJpa.findByTmdbId(movie.getTmdbId())
                        .orElseGet(() -> {
                            MovieEntity m = new MovieEntity();
                            m.setTmdbId(movie.getTmdbId());
                            m.setTitle(movie.getTitle());
                            if (movie.getYear() != null) m.setYear(movie.getYear().shortValue());
                            m.setDirector(movie.getDirector());
                            m.setGenres(movie.getGenres());
                            return movieJpa.save(m);
                        });
                entity.getMovies().add(movieEntity);
            }
        }

        // Guardar la canción (necesario antes de las relaciones con @MapsId)
        SongEntity saved = jpa.save(entity);

        // Audio features: @MapsId requiere el song ya persistido
        if (song.getAudioFeatures() != null) {
            AudioFeaturesEntity af = audioFeaturesMapper.toEntity(song.getAudioFeatures());
            af.setSong(saved);
            audioFeaturesJpa.save(af);
        }

        // Tags: @MapsId requiere el song ya persistido
        if (song.getTags() != null) {
            for (Tag tag : song.getTags()) {
                if (tag.getName() == null || tag.getName().isBlank()) continue;
                SongTagEntity tagEntity = new SongTagEntity();
                tagEntity.setId(new SongTagId(saved.getId(), tag.getName()));
                tagEntity.setSong(saved);
                tagEntity.setWeight(tag.getWeight());
                songTagJpa.save(tagEntity);
            }
        }

        return mapper.toModel(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Song> findByTitleContaining(String title) {
        return jpa.findByTitleContainingIgnoreCase(title).stream()
                .map(mapper::toModel)
                .toList();
    }

    private void addMoviesToSong(SongEntity songEntity, List<Movie> movies) {
        for (Movie movie : movies) {
            if (movie.getTmdbId() == null || movie.getTitle() == null) continue;
            MovieEntity movieEntity = movieJpa.findByTmdbId(movie.getTmdbId())
                    .orElseGet(() -> {
                        MovieEntity m = new MovieEntity();
                        m.setTmdbId(movie.getTmdbId());
                        m.setTitle(movie.getTitle());
                        if (movie.getYear() != null) m.setYear(movie.getYear().shortValue());
                        m.setDirector(movie.getDirector());
                        m.setGenres(movie.getGenres());
                        return movieJpa.save(m);
                    });
            songEntity.getMovies().add(movieEntity);
        }
    }

    /** Persiste los tags nuevos de una canción ya existente, ignorando los que ya tenga. */
    private void addMissingTags(SongEntity song, List<Tag> tags) {
        if (tags == null || tags.isEmpty()) return;
        for (Tag tag : tags) {
            if (tag.getName() == null || tag.getName().isBlank()) continue;
            SongTagId id = new SongTagId(song.getId(), tag.getName());
            if (!songTagJpa.existsById(id)) {
                SongTagEntity tagEntity = new SongTagEntity();
                tagEntity.setId(id);
                tagEntity.setSong(song);
                tagEntity.setWeight(tag.getWeight());
                songTagJpa.save(tagEntity);
            }
        }
    }

    private AlbumEntity findOrCreateAlbum(es.wara.ecos.backend.domain.model.Album album) {
        return albumJpa.findBySpotifyId(album.getSpotifyId())
                .orElseGet(() -> {
                    AlbumEntity a = new AlbumEntity();
                    a.setSpotifyId(album.getSpotifyId());
                    a.setTitle(album.getTitle());
                    if (album.getYear() != null) a.setYear(album.getYear().shortValue());
                    a.setCoverUrl(album.getCoverUrl());
                    return albumJpa.save(a);
                });
    }

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

}
