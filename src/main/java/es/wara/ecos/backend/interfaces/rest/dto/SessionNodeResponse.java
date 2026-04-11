package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.SessionNode;

import java.util.UUID;

/**
 * DTO de respuesta para una canción dentro de una sesión de exploración.
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
public record SessionNodeResponse(
        UUID id,
        SongResponse song,
        Integer sequenceOrder
) {

    public static SessionNodeResponse from(SessionNode node) {
        return new SessionNodeResponse(
                node.getId(),
                node.getSong() != null ? SongResponse.from(node.getSong()) : null,
                node.getSequenceOrder()
        );
    }

}
