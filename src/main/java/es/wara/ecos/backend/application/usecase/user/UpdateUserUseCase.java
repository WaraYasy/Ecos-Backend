package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.exception.ConflictException;
import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso para actualizar los datos de un usuario (username y/o idioma).
 *
 * <p>Semántica PATCH: solo se modifican los campos con valor no nulo.
 * Si el nuevo username ya está en uso por otro usuario, lanza {@link ConflictException}.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class UpdateUserUseCase {

    private final UserRepository userRepository;

    public UpdateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Actualiza los datos del usuario indicado.
     *
     * @param userId   identificador del usuario a modificar
     * @param username nuevo nombre de usuario, o null para no cambiarlo
     * @param language nuevo idioma preferido, o null para no cambiarlo
     * @return el usuario actualizado
     * @throws IllegalArgumentException si el usuario no existe
     * @throws ConflictException        si el nuevo username ya está en uso por otro usuario
     */
    public User execute(UUID userId, String username, String language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        if (username != null && !username.isBlank()) {
            userRepository.findByUsername(username)
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(__ -> {
                        throw new ConflictException("El nombre de usuario ya está en uso");
                    });
            user.setUsername(username);
        }

        if (language != null && !language.isBlank()) {
            user.setLanguage(language);
        }

        return userRepository.save(user);
    }
}