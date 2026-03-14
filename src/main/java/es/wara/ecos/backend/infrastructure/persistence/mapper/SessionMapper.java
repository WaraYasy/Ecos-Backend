package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.infrastructure.persistence.entity.ExplorationSessionEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper que convierte entre ExplorationSessionEntity y el modelo de dominio ExplorationSession.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class SessionMapper {

    public ExplorationSession toModel(ExplorationSessionEntity entity) {
        if (entity == null) return null;
        ExplorationSession session = new ExplorationSession();
        session.setId(entity.getId());
        session.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        session.setStartSongId(entity.getStartSong() != null ? entity.getStartSong().getId() : null);
        session.setCreatedAt(entity.getCreatedAt());
        return session;
    }

    public ExplorationSessionEntity toEntity(ExplorationSession model) {
        if (model == null) return null;
        ExplorationSessionEntity entity = new ExplorationSessionEntity();
        entity.setId(model.getId());
        entity.setCreatedAt(model.getCreatedAt());
        return entity;
    }

}
