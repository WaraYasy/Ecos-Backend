package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper que convierte SongTagEntity al modelo de dominio Tag (solo conversión toModel).
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class TagMapper {

    public Tag toModel(SongTagEntity entity) {
        if (entity == null) return null;
        return new Tag(
                entity.getId().getTag(),
                entity.getWeight()
        );
    }

}
