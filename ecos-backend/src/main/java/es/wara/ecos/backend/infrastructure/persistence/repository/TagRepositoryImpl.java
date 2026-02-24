package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongTagRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.TagMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Componente interno de infraestructura para consultas de song_tags agrupadas por canción.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Repository
public class TagRepositoryImpl {

    private final JpaSongTagRepository jpa;
    private final TagMapper mapper;

    public TagRepositoryImpl(JpaSongTagRepository jpa, TagMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public List<Tag> findBySongId(Long songId) {
        return jpa.findById_SongId(songId).stream()
                .map(mapper::toModel)
                .toList();
    }

}
