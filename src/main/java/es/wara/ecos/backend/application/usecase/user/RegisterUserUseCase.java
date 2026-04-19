package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.exception.ConflictException;
import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.PasswordHasher;
import es.wara.ecos.backend.domain.port.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Caso de uso para registrar un nuevo usuario en ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Registra y persiste un nuevo usuario con su contraseña hasheada.
     *
     * @param username nombre de usuario único
     * @param email    dirección de correo electrónico única
     * @param password contraseña en texto plano (se hashea con BCrypt antes de persistir)
     * @param language código de idioma preferido (ej. "es", "en")
     * @return el usuario creado con su identificador asignado
     * @throws IllegalArgumentException si el nombre de usuario o el email ya están en uso
     */
    public User execute(String username, String email, String password, String language) {
        log.info("Intento de registro: username='{}', email='{}'", username, email);

        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Registro rechazado — username ya en uso: '{}'", username);
            throw new ConflictException("El nombre de usuario ya está en uso: " + username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Registro rechazado — email ya en uso: '{}'", email);
            throw new ConflictException("El email ya está en uso: " + email);
        }

        String userCode;
        int codeAttempts = 0;
        do {
            userCode = UserCodeGenerator.generate();
            if (++codeAttempts > 5) throw new IllegalStateException("No se pudo generar un código de usuario único");
        } while (userRepository.existsByUserCode(userCode));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHasher.hash(password));
        user.setLanguage(language);
        user.setCreatedAt(LocalDateTime.now());
        user.setUserCode(userCode);
        User saved = userRepository.save(user);
        log.info("Usuario registrado: id='{}', username='{}', language='{}'", saved.getId(), username, language);
        return saved;
    }

}
