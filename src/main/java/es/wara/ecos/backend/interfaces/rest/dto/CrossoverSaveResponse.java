package es.wara.ecos.backend.interfaces.rest.dto;

import java.util.UUID;

/**
 * Respuesta al guardar un crossover como playlist.
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
public record CrossoverSaveResponse(UUID playlistId, String playlistName, int songCount) {}