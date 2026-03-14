package es.wara.ecos.backend.application.usecase.search;

import java.math.BigDecimal;

/**
 * Parámetros de búsqueda multidimensional para el sistema de 5 cuadrantes de ECOS.
 *
 * <p>Todos los campos son opcionales salvo {@code limit} y {@code offset}.
 * Los cuadrantes activos son los que tienen valor no nulo:</p>
 * <ul>
 *   <li><b>Texto libre:</b> {@code q}</li>
 *   <li><b>Estética:</b> {@code aesthetic}</li>
 *   <li><b>Temporal:</b> {@code decade} (ej. "1980s")</li>
 *   <li><b>Cinematográfico:</b> {@code movie}</li>
 *   <li><b>Técnico:</b> {@code instrumental}, {@code texture}, {@code mode},
 *       rangos de energía, valencia y BPM</li>
 * </ul>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record SearchParams(
        String q,
        String aesthetic,
        String decade,
        String movie,
        Boolean instrumental,
        String texture,
        String mode,
        BigDecimal energyMin, BigDecimal energyMax,
        BigDecimal valenceMin, BigDecimal valenceMax,
        BigDecimal bpmMin,    BigDecimal bpmMax,
        int limit,
        int offset
) {

    /** Constructor con valores por defecto para limit (20) y offset (0). */
    public SearchParams {
        if (limit <= 0) limit = 20;
        if (offset < 0) offset = 0;
    }
}
