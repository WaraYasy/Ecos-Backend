package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de petición para iniciar una nueva sesión de exploración.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record CreateSessionRequest(
        @NotNull Long userId,
        @NotNull Long startSongId
) {}
