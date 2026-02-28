package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.application.usecase.search.SearchParams;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SongRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.SongMapper;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import es.wara.ecos.backend.infrastructure.persistence.specification.SongSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del puerto de dominio SongRepository usando JPA.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
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
    public Song save(Song song) {
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
