package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.application.usecase.search.SearchParams;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SongRepository;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.SongMapper;
import es.wara.ecos.backend.infrastructure.persistence.specification.SongSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del puerto de dominio SongRepository usando JPA.
 * <p>
 * El método {@link #save(Song)} aplica deduplicación por ISRC antes de insertar:
 * si ya existe una canción con el mismo ISRC, se devuelve la existente (actualizando
 * el spotifyId si cambió). Esto evita duplicados entre versiones remasterizadas o
 * regionales de la misma grabación.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
@Slf4j
@Repository
public class SongRepositoryImpl implements SongRepository {

    private final JpaSongRepository jpa;
    private final SongMapper mapper;

    public SongRepositoryImpl(JpaSongRepository jpa, SongMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<Song> findById(Long id) {
        return jpa.findById(id).map(mapper::toModel);
    }

    @Override
    public Optional<Song> findBySpotifyId(String spotifyId) {
        return jpa.findBySpotifyId(spotifyId).map(mapper::toModel);
    }

    @Override
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
    public Song save(Song song) {
        // 1. Dedup por ISRC (grabación física)
        if (song.getIsrc() != null) {
            Optional<SongEntity> byIsrc = jpa.findByIsrc(song.getIsrc());
            if (byIsrc.isPresent()) {
                SongEntity existing = byIsrc.get();
                if (song.getSpotifyId() != null
                        && !song.getSpotifyId().equals(existing.getSpotifyId())) {
                    log.info("ISRC {} ya existe con spotifyId={}, actualizando a {}",
                            song.getIsrc(), existing.getSpotifyId(), song.getSpotifyId());
                    existing.setSpotifyId(song.getSpotifyId());
                    jpa.save(existing);
                }
                return mapper.toModel(existing);
            }
        }

        // 2. Dedup por spotifyId (misma edición de Spotify)
        if (song.getSpotifyId() != null) {
            Optional<SongEntity> bySpotifyId = jpa.findBySpotifyId(song.getSpotifyId());
            if (bySpotifyId.isPresent()) {
                return mapper.toModel(bySpotifyId.get());
            }
        }

        // 3. Nueva canción — insertar
        return mapper.toModel(jpa.save(mapper.toEntity(song)));
    }

    @Override
    public List<Song> findByTitleContaining(String title) {
        return jpa.findByTitleContainingIgnoreCase(title).stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public List<Song> search(SearchParams params) {
        Specification<SongEntity> spec = SongSpecification.from(params);
        int pageNumber = params.limit() > 0 ? params.offset() / params.limit() : 0;
        PageRequest page = PageRequest.of(pageNumber, Math.max(params.limit(), 1));
        return jpa.findAll(spec, page).stream()
                .map(mapper::toModel)
                .toList();
    }

}
