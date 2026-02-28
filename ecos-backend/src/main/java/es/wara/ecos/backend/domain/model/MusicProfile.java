package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Perfil musical calculado de un usuario a partir de su historial de escucha
 * y sesiones de exploración en ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MusicProfile {

    private Long userId;
    /** Energía media de las canciones escuchadas: 0.0 a 1.0. */
    private BigDecimal avgEnergy;
    /** Variedad de tonalidades presentes en el historial: 0.0 a 1.0. */
    private BigDecimal harmonicRichness;
    /** Diversidad de géneros musicales escuchados: 0.0 a 1.0. */
    private BigDecimal eclecticity;
    /** Distribución de escuchas por décadas, ej. {"1980s": 0.4, "1990s": 0.3}. */
    private Map<String, Double> favoriteEras;
    private List<String> topGenres;

}
