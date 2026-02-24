package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para obtener los datos de perfil de un usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class GetUserProfileUseCase {

    private final UserRepository userRepository;

    public GetUserProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Devuelve el usuario con el identificador dado.
     *
     * @param userId identificador del usuario
     * @return el usuario encontrado
     * @throws IllegalArgumentException si el usuario no existe
     */
    public User execute(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
    }

}
