package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Recomendación musical generada por ECOS para un usuario, con la puntuación
 * de similitud y la justificación del resultado.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MusicRecommendation {

    private Song song;
    /** Puntuación de similitud respecto a la canción de referencia: 0.0 a 1.0. */
    private BigDecimal similarityScore;
    /** Motivo de la recomendación, ej. "similar_audio", "shared_tags", "same_era". */
    private String reason;

}
