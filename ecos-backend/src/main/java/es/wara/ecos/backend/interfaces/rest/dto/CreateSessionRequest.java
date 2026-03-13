package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO de petición para iniciar una nueva sesión de exploración.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record CreateSessionRequest(
        @NotNull UUID startSongId
) {}
