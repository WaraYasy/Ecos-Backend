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

    /** Clase utilitaria: constructor privado para evitar instanciación accidental. */
    private SecurityUtils() {}

    /**
     * Devuelve el identificador del usuario autenticado en la petición actual.
     * <p>
     * El {@code Authentication} lo establece {@link JwtAuthFilter} durante el
     * procesamiento del filtro, usando el {@code userId} como nombre del principal.
     * </p>
     *
     * @return userId extraído del token JWT
     * @throws IllegalStateException si no hay usuario autenticado en el contexto
     *                               (petición pública o token ausente/inválido)
     */
    public static Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        // El contexto está vacío si la petición no portaba un token válido
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("No hay usuario autenticado en el contexto de seguridad");
        }

        // El nombre del principal es el userId almacenado como String en JwtAuthFilter
        return Long.parseLong(auth.getName());
    }

}
