package es.wara.ecos.backend.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Resultado de construir o expandir el grafo neuronal para una canción.
 * Los nodos contienen las canciones con su posición visual y sus aristas.
 * Las aristas se listan también de forma plana para facilitar el serializado REST.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record GraphResponse(
        UUID rootSongId,
        List<GraphNode> nodes,
        List<GraphEdge> edges
) {}