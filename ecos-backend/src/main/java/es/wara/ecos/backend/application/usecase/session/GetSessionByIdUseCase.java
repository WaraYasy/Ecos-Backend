package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.port.SessionRepository;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para obtener una sesión de exploración por su identificador.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class GetSessionByIdUseCase {

    private final SessionRepository sessionRepository;

    public GetSessionByIdUseCase(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Devuelve la sesión con el identificador dado.
     *
     * @param id identificador de la sesión
     * @return la sesión encontrada
     * @throws IllegalArgumentException si la sesión no existe
     */
    public ExplorationSession execute(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada: " + id));
    }

}
