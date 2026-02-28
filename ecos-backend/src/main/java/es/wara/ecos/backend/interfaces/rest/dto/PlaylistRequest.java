package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de petición para crear o actualizar una playlist.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record PlaylistRequest(
        @NotBlank String name,
        String description,
        @NotNull Long userId
) {}
