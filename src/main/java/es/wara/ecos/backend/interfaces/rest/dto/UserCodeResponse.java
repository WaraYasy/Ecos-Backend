package es.wara.ecos.backend.interfaces.rest.dto;

import java.util.UUID;

/**
 * Respuesta mínima al resolver un código de usuario para crossover.
 * Solo expone userId y username — nunca email ni datos privados.
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
public record UserCodeResponse(UUID userId, String username) {}