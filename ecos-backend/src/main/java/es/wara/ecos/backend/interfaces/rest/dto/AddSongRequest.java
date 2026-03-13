package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO de petición para añadir una canción a una playlist.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record AddSongRequest(
        @NotNull UUID songId
) {}
