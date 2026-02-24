package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Caso de uso para registrar un nuevo usuario en ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;

    public RegisterUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Registra y persiste un nuevo usuario.
     *
     * @param username nombre de usuario único
     * @param email    dirección de correo electrónico única
     * @param language código de idioma preferido (ej. "es", "en")
     * @return el usuario creado con su identificador asignado
     * @throws IllegalArgumentException si el nombre de usuario o el email ya están en uso
     */
    public User execute(String username, String email, String language) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso: " + username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("El email ya está en uso: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setLanguage(language);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

}
