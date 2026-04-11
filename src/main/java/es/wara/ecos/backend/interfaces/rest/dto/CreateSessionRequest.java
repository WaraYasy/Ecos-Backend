package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * DTO de petición para guardar una sesión de exploración.
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
public record CreateSessionRequest(
        @NotNull UUID startSongId,
        @NotEmpty List<UUID> songIds,
        String name
) {}
