package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.ExplorationSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para una sesión de exploración musical.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record SessionResponse(
        UUID id,
        UUID userId,
        UUID startSongId,
        String name,
        LocalDateTime createdAt,
        List<SessionNodeResponse> songs
) {

    public static SessionResponse from(ExplorationSession session) {
        return new SessionResponse(
                session.getId(),
                session.getUserId(),
                session.getStartSongId(),
                session.getName(),
                session.getCreatedAt(),
                session.getNodes() != null
                        ? session.getNodes().stream().map(SessionNodeResponse::from).toList()
                        : List.of()
        );
    }

}
