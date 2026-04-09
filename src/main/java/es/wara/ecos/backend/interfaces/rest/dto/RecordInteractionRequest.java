package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Cuerpo de la petición para registrar una interacción del usuario con una canción.
 *
 * <p>Valores válidos para {@code action}: {@code play}, {@code skip},
 * {@code add_playlist}, {@code expand_node}.</p>
 *
 * @param songId     UUID de la canción con la que interactuó el usuario
 * @param action     tipo de interacción (debe coincidir con el ENUM {@code interaction_action} de PostgreSQL)
 * @param durationMs milisegundos escuchados; relevante cuando {@code action=play}, ignorado en los demás casos
 */
public record RecordInteractionRequest(
        @NotNull UUID songId,
        @NotNull String action,
        Integer durationMs
) {}