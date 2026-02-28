package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de petición para registrar un nuevo usuario en ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String email,
        @NotBlank String password,
        String language
) {}
