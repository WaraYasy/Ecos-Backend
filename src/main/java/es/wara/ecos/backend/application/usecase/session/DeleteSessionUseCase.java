package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.port.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso para eliminar una sesión de exploración guardada.
 * Solo el propietario puede eliminar su sesión.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class DeleteSessionUseCase {

    private final SessionRepository sessionRepository;

    public DeleteSessionUseCase(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Elimina la sesión indicada si el usuario es su propietario.
     *
     * @param sessionId     identificador de la sesión a eliminar
     * @param currentUserId usuario autenticado
     * @throws IllegalArgumentException si la sesión no existe
     * @throws ForbiddenException       si el usuario no es el propietario
     */
    public void execute(UUID sessionId, UUID currentUserId) {
        ExplorationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada: " + sessionId));

        if (!session.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("No tienes permiso para eliminar esta sesión");
        }

        sessionRepository.delete(sessionId);
    }

}