package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Nodo del grafo de exploración musical.
 * Contiene la canción representada, su posición visual calculada por el backend,
 * el score de similitud respecto al nodo raíz, y sus aristas hacia canciones relacionadas.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode {

    private Song song;
    /** Coordenada X calculada por GraphLayoutService (force-directed). */
    private float xPosition;
    /** Coordenada Y calculada por GraphLayoutService (force-directed). */
    private float yPosition;
    /** Similitud con el nodo raíz (0.0 a 1.0). 1.0 para el propio nodo raíz. */
    private float similarity;
    /** Aristas hacia canciones relacionadas (máximo 8). */
    private List<GraphEdge> edges;

}