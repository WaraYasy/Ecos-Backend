package es.wara.ecos.backend.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que protege {@code /v1/admin/**} mediante una API key estática.
 *
 * <p>El cliente debe enviar la cabecera {@code X-Admin-Key} con el valor
 * configurado en {@code admin.api-key}. Si la cabecera falta o no coincide
 * se devuelve {@code 401}. El filtro no actúa sobre ninguna otra ruta.</p>
 *
 * <p>Se ejecuta antes de {@link JwtAuthFilter} gracias a {@code @Order(1)},
 * por lo que las rutas admin no pasan por la validación JWT.</p>
 */
@Component
@Order(1)
public class AdminApiKeyFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH   = "/v1/admin/";
    private static final String HEADER_NAME  = "X-Admin-Key";

    @Value("${admin.api-key:}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(ADMIN_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (adminApiKey.isBlank()) {
            sendError(response, "Admin API key no configurada en el servidor");
            return;
        }

        String provided = request.getHeader(HEADER_NAME);
        if (provided == null || !adminApiKey.equals(provided)) {
            sendError(response, "X-Admin-Key inválida o ausente");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"error\":\"UNAUTHORIZED\",\"message\":\"" + message + "\",\"status\":401}");
    }
}
