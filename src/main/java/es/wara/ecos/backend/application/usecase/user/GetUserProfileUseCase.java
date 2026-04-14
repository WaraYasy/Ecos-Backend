package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.UUID;

/**
 * Caso de uso para obtener los datos de perfil de un usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
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
    public User execute(UUID userId) {
        log.debug("GetUserProfile: buscando usuario userId={}", userId);
        StopWatch sw = new StopWatch();
        sw.start();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        sw.stop();
        long ms = sw.getTotalTimeMillis();
        if (ms > 200) {
            log.warn("GetUserProfile: consulta lenta userId={} → {}ms", userId, ms);
        } else {
            log.debug("GetUserProfile: completado userId={} → {}ms", userId, ms);
        }
        return user;
    }

}
