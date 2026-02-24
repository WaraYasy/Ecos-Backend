package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper que convierte entre UserEntity y el modelo de dominio User.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class UserMapper {

    public User toModel(UserEntity entity) {
        if (entity == null) return null;
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getLanguage(),
                entity.getCreatedAt()
        );
    }

    public UserEntity toEntity(User model) {
        if (model == null) return null;
        UserEntity entity = new UserEntity();
        entity.setId(model.getId());
        entity.setUsername(model.getUsername());
        entity.setEmail(model.getEmail());
        entity.setLanguage(model.getLanguage());
        entity.setCreatedAt(model.getCreatedAt());
        return entity;
    }

}
