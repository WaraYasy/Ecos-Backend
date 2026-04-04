package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.SongEmbedding;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongEmbeddingEntity;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

/**
 * Mapper entre SongEmbeddingEntity y el modelo de dominio SongEmbedding.
 * Convierte float[] ↔ String en formato pgvector "[v1,v2,...,vN]".
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class SongEmbeddingMapper {

    public SongEmbedding toModel(SongEmbeddingEntity entity) {
        if (entity == null) return null;
        return new SongEmbedding(
                entity.getSongId(),
                parseVector(entity.getEmbedding()),
                entity.getModelVersion(),
                entity.getUpdatedAt()
        );
    }

    public SongEmbeddingEntity toEntity(SongEmbedding model) {
        if (model == null) return null;
        SongEmbeddingEntity entity = new SongEmbeddingEntity();
        entity.setSongId(model.getSongId());
        entity.setEmbedding(formatVector(model.getEmbedding()));
        entity.setModelVersion(model.getModelVersion());
        entity.setUpdatedAt(model.getUpdatedAt());
        return entity;
    }

    /** Serializa float[] al formato pgvector: "[v1,v2,...,vN]". */
    public static String formatVector(float[] vector) {
        if (vector == null) return null;
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float v : vector) {
            joiner.add(String.valueOf(v));
        }
        return joiner.toString();
    }

    /** Parsea el formato pgvector "[v1,v2,...,vN]" a float[]. */
    private static float[] parseVector(String raw) {
        if (raw == null) return null;
        String inner = raw.substring(1, raw.length() - 1);
        String[] parts = inner.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

}