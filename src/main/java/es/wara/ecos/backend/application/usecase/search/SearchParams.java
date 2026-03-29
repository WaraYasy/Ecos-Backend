package es.wara.ecos.backend.application.usecase.search;

import java.math.BigDecimal;

/**
 * Parámetros de búsqueda multidimensional para el sistema de 5 cuadrantes de ECOS.
 *
 * <p>Todos los campos son opcionales salvo {@code pageSize} y {@code page}.
 * Los cuadrantes activos son los que tienen valor no nulo:</p>
 * <ul>
 *   <li><b>Texto libre:</b> {@code q}</li>
 *   <li><b>Estética:</b> {@code aesthetic}</li>
 *   <li><b>Género musical:</b> {@code genre} (ej. "rock", "jazz", "indie")</li>
 *   <li><b>Temporal:</b> {@code decade} (ej. "1980s")</li>
 *   <li><b>Cinematográfico:</b> {@code movie}</li>
 *   <li><b>Técnico:</b> {@code instrumental}, {@code texture}, {@code mode},
 *       rangos de energía, valencia y BPM</li>
 * </ul>
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
public record SearchParams(
        String q,
        String aesthetic,
        String genre,
        String decade,
        String movie,
        Boolean instrumental,
        String texture,
        String mode,
        BigDecimal energyMin, BigDecimal energyMax,
        BigDecimal valenceMin, BigDecimal valenceMax,
        BigDecimal bpmMin,    BigDecimal bpmMax,
        int pageSize,
        int page
) {

    /** pageSize por defecto 20, máximo 50. page por defecto 0. */
    public SearchParams {
        if (pageSize <= 0) pageSize = 20;
        if (pageSize > 50) pageSize = 50;
        if (page < 0) page = 0;
    }
}
