package es.wara.ecos.backend.interfaces.rest.dto;

import java.util.UUID;

/**
 * DTO de respuesta al inicio de sesión con el token JWT y el identificador del usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record LoginResponse(
        String token,
        UUID userId,
        boolean accountLinked
) {}
