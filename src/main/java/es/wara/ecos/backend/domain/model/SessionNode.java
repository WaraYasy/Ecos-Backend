package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa un nodo dentro de una sesión de exploración, indicando la canción
 * visitada y su posición en el grafo de descubrimiento.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionNode {

    private UUID id;
    private UUID sessionId;
    private Song song;
    /** Coordenada X del nodo en el grafo visual. */
    private BigDecimal xPosition;
    /** Coordenada Y del nodo en el grafo visual. */
    private BigDecimal yPosition;
    private LocalDateTime visitedAt;
    /** Orden de visita dentro de la sesión, comenzando en 1. */
    private Integer sequenceOrder;

}
