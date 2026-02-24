package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.port.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso para obtener el historial de sesiones de exploración de un usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class GetSessionHistoryUseCase {

    private final SessionRepository sessionRepository;

    public GetSessionHistoryUseCase(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Devuelve todas las sesiones de exploración de un usuario.
     *
     * @param userId identificador del usuario
     * @return lista de sesiones del usuario, nunca null
     */
    public List<ExplorationSession> execute(Long userId) {
        return sessionRepository.findByUserId(userId);
    }

}
