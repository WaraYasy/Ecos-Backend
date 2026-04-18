package es.wara.ecos.backend.interfaces.rest.dto;

import java.util.UUID;

/**
 * DTO de respuesta al inicio de sesión con el token JWT y el identificador del usuario.
 *
 * @param token         JWT de sesión (24h por defecto)
 * @param userId        identificador del usuario en ECOS
 * @param accountLinked {@code true} si el login con Spotify ha vinculado una cuenta local
 *                      existente por coincidencia de email; el front puede mostrar un aviso
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
