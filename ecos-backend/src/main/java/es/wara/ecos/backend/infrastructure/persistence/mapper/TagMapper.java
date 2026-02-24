package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagEntity;
import org.springframework.stereotype.Component;

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