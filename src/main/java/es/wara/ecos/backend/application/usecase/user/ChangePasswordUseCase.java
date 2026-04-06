package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.PasswordHasher;
import es.wara.ecos.backend.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso para cambiar la contraseña de un usuario.
 *
 * <p>Exige la contraseña actual como verificación de identidad antes de aplicar el cambio.
 * El error por contraseña incorrecta usa el mismo mensaje genérico que el login para
 * no filtrar si el usuario existe o si la contraseña es incorrecta.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public ChangePasswordUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Cambia la contraseña del usuario tras verificar la contraseña actual.
     *
     * @param userId          identificador del usuario
     * @param currentPassword contraseña actual en texto plano para verificar identidad
     * @param newPassword     nueva contraseña en texto plano
     * @throws IllegalArgumentException si el usuario no existe
     * @throws IllegalStateException    si la contraseña actual es incorrecta
     */
    public void execute(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        if (user.getPasswordHash() == null) {
            // Usuario autenticado solo con Spotify — no tiene contraseña local
            throw new IllegalStateException("Credenciales incorrectas");
        }
        if (!passwordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalStateException("Credenciales incorrectas");
        }

        user.setPasswordHash(passwordHasher.hash(newPassword));
        userRepository.save(user);
    }
}