package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.domain.model.SongEmbedding;
import es.wara.ecos.backend.domain.port.SongEmbeddingRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongEmbeddingRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.SongEmbeddingMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de infraestructura que implementa SongEmbeddingRepository usando JPA + pgvector.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Repository
public class SongEmbeddingRepositoryImpl implements SongEmbeddingRepository {

    private final JpaSongEmbeddingRepository jpa;
    private final SongEmbeddingMapper mapper;

    public SongEmbeddingRepositoryImpl(JpaSongEmbeddingRepository jpa, SongEmbeddingMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<SongEmbedding> findBySongId(UUID songId) {
        return jpa.findById(songId).map(mapper::toModel);
    }

    @Override
    public List<SongEmbedding> findAllBySongIds(List<UUID> songIds) {
        if (songIds == null || songIds.isEmpty()) return List.of();
        return jpa.findAllById(songIds).stream()
                .map(mapper::toModel)
                .toList();
    }

    @Override
    public void save(SongEmbedding embedding) {
        jpa.save(mapper.toEntity(embedding));
    }

    @Override
    public List<UUID> findNearestNeighbors(float[] queryVector, int limit, List<UUID> excludeIds) {
        String vectorStr = SongEmbeddingMapper.formatVector(queryVector);
        // NOT IN () con lista vacía genera SQL inválido en PostgreSQL; se usa versión sin exclusiones
        if (excludeIds == null || excludeIds.isEmpty()) {
            return jpa.findNearestNeighborsNoExclusions(vectorStr, limit);
        }
        return jpa.findNearestNeighbors(vectorStr, excludeIds, limit);
    }

}