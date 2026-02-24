package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Nodo del grafo de exploración musical, con su posición visual y las canciones
 * conectadas por similitud.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode {

    private Song song;
    /** Coordenada X del nodo en el grafo visual. */
    private BigDecimal xPosition;
    /** Coordenada Y del nodo en el grafo visual. */
    private BigDecimal yPosition;
    /** Canciones relacionadas que forman las aristas del grafo. */
    private List<Song> connections;

}
