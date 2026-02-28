package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de petición para actualizar las preferencias de un usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record UpdatePreferencesRequest(
        @NotBlank String language
) {}
