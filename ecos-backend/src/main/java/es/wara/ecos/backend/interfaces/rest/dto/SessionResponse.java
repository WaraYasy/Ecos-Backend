package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.ExplorationSession;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para una sesión de exploración musical.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record SessionResponse(
        Long id,
        Long userId,
        Long startSongId,
        LocalDateTime createdAt,
        List<SessionNodeResponse> nodes
) {

    public static SessionResponse from(ExplorationSession session) {
        return new SessionResponse(
                session.getId(),
                session.getUserId(),
                session.getStartSongId(),
                session.getCreatedAt(),
                session.getNodes() != null
                        ? session.getNodes().stream().map(SessionNodeResponse::from).toList()
                        : List.of()
        );
    }

}
