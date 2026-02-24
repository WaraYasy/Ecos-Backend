package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Etiqueta de género o estilo asociada a una canción, con su relevancia ponderada.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    private String name;
    /** Relevancia de la etiqueta para la canción: 0.0 a 1.0. */
    private BigDecimal weight;

}
