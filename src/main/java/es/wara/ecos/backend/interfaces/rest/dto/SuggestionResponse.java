package es.wara.ecos.backend.interfaces.rest.dto;

/**
 * Sugerencia de autocompletado para los campos de búsqueda del frontend.
 *
 * @param value valor que se enviará como parámetro de búsqueda (ej. "The Avengers")
 * @param label texto legible para mostrar al usuario (ej. "The Avengers (2012)")
 */
public record SuggestionResponse(String value, String label) {}
