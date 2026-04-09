package es.wara.ecos.backend.interfaces.rest.dto;

import java.util.List;

/**
 * Respuesta del endpoint de recomendaciones cruzadas.
 *
 * @param overlapCount número de canciones que ambos usuarios tienen en común en su historial
 * @param songs        canciones recomendadas enriquecidas con datos completos
 */
public record CrossoverResponse(int overlapCount, List<SongResponse> songs) {}