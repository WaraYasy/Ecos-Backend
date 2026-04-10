package es.wara.ecos.backend.interfaces.rest.dto;

/**
 * DTO de petición para actualizar una playlist (PATCH).
 * Ambos campos son opcionales — solo se modifican los que tienen valor no nulo.
 *
 * @param name        nuevo nombre de la playlist; null = no cambiar
 * @param description nueva descripción; null = no cambiar
 */
public record UpdatePlaylistRequest(String name, String description) {}