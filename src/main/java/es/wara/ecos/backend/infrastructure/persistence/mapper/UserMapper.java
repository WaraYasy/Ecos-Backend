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
        User user = new User();
        user.setId(entity.getId());
        user.setUsername(entity.getUsername());
        user.setEmail(entity.getEmail());
        user.setLanguage(entity.getLanguage());
        user.setPasswordHash(entity.getPasswordHash());
        user.setAuthProvider(entity.getAuthProvider());
        user.setProviderId(entity.getProviderId());
        user.setCreatedAt(entity.getCreatedAt());
        return user;
    }

    public UserEntity toEntity(User model) {
        if (model == null) return null;
        UserEntity entity = new UserEntity();
        entity.setId(model.getId());
        entity.setUsername(model.getUsername());
        entity.setEmail(model.getEmail());
        entity.setLanguage(model.getLanguage());
        entity.setPasswordHash(model.getPasswordHash());
        entity.setAuthProvider(model.getAuthProvider() != null ? model.getAuthProvider() : "local");
        entity.setProviderId(model.getProviderId());
        entity.setCreatedAt(model.getCreatedAt());
        return entity;
    }

}
