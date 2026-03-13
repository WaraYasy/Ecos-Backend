package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.port.PasswordHasher;
import es.wara.ecos.backend.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso para autenticar a un usuario con su email y contraseña.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Autentica al usuario y devuelve su identificador.
     *
     * @param email    dirección de correo electrónico del usuario
     * @param password contraseña en texto plano
     * @return identificador del usuario autenticado
     * @throws IllegalArgumentException si las credenciales son incorrectas
     */
    public UUID execute(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordHasher.matches(password, user.getPasswordHash()))
                .map(user -> user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));
    }

}
