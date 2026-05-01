package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Arista del grafo neuronal que conecta dos canciones con un peso de similitud.
 * El tipo indica qué componente de la fórmula de similitud fue dominante.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public class GraphEdge {

    /** Canción origen. */
    private UUID sourceId;
    /** Canción destino. */
    private UUID targetId;
    /** Similitud total entre las dos canciones (0.0 a 1.0). */
    private float weight;
    /** Componente de similitud dominante que origina esta conexión. */
    private EdgeType type;

    /** Tipos de arista según el componente de similitud dominante. */
    public enum EdgeType {
        /** Similitud dominada por características de audio (energy, valence, BPM…). */
        AUDIO,
        /** Similitud dominada por embeddings semánticos (pgvector). */
        SEMANTIC,
        /** Similitud dominada por etiquetas Last.fm compartidas. */
        TAG,
        /** Similitud dominada por cercanía temporal entre las canciones. */
        ERA,
        /** Similitud dominada por películas compartidas en las que aparecen. */
        MOVIE
    }

}