package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Etiqueta de género o estilo asociada a una canción, con su relevancia ponderada.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    private String name;
    /** Relevancia de la etiqueta para la canción: 0.0 a 1.0. */
    private BigDecimal weight;

}
