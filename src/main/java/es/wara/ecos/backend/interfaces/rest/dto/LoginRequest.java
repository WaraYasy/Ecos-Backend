package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de petición para el inicio de sesión con email y contraseña.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {}
