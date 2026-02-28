package es.wara.ecos.backend.infrastructure.security;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper estático para acceder al usuario autenticado desde el {@link SecurityContextHolder}.
 * <p>
 * El nombre del principal es el {@code userId} en formato String, tal como lo
 * establece {@link JwtAuthFilter}.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Devuelve el identificador del usuario autenticado en la petición actual.
     *
     * @return userId extraído del token JWT
     * @throws IllegalStateException si no hay usuario autenticado en el contexto
     */
    public static Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("No hay usuario autenticado en el contexto de seguridad");
        }
        return Long.parseLong(auth.getName());
    }

}
