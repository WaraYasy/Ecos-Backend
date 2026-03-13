package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso para actualizar las preferencias de un usuario (actualmente, idioma).
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class UpdateUserPreferencesUseCase {

    private final UserRepository userRepository;

    public UpdateUserPreferencesUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Actualiza el idioma preferido del usuario.
     *
     * @param userId   identificador del usuario a actualizar
     * @param language nuevo código de idioma (ej. "es", "en")
     * @return el usuario actualizado
     * @throws IllegalArgumentException si el usuario no existe
     */
    public User execute(UUID userId, String language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        user.setLanguage(language);
        return userRepository.save(user);
    }

}
