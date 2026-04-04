package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Embedding semántico de una canción, representado como vector de 384 dimensiones.
 * Generado por el microservicio IA y usado para calcular similitud semántica en el grafo.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SongEmbedding {

    private UUID songId;
    /** Vector de 384 dimensiones generado por el microservicio IA. */
    private float[] embedding;
    /** Versión del modelo que generó el embedding (ej. "all-MiniLM-L6-v2"). */
    private String modelVersion;
    private LocalDateTime updatedAt;

}