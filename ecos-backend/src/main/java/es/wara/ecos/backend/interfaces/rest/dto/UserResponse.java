package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.User;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con los datos básicos de un usuario de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String language,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getLanguage(),
                user.getCreatedAt()
        );
    }

}
