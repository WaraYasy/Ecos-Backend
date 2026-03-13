package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.SessionNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para un nodo dentro de una sesión de exploración.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record SessionNodeResponse(
        UUID id,
        SongResponse song,
        BigDecimal xPosition,
        BigDecimal yPosition,
        LocalDateTime visitedAt,
        Integer sequenceOrder
) {

    public static SessionNodeResponse from(SessionNode node) {
        return new SessionNodeResponse(
                node.getId(),
                node.getSong() != null ? SongResponse.from(node.getSong()) : null,
                node.getXPosition(),
                node.getYPosition(),
                node.getVisitedAt(),
                node.getSequenceOrder()
        );
    }

}
