package es.wara.ecos.backend.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Filtro de rate limiting para la API de ECOS.
 *
 * <p>Ejecuta <b>después</b> de {@link JwtAuthFilter}, por lo que el userId ya está
 * disponible en el {@link SecurityContextHolder} cuando este filtro actúa.</p>
 *
 * <p>Estrategia de limitación:</p>
 * <ul>
 *   <li><b>Endpoints públicos</b> (login, registro): limitados por IP — protegen
 *       contra fuerza bruta y spam de cuentas.</li>
 *   <li><b>{@code GET /v1/search}</b>: 30 req/min por userId — el endpoint más costoso
 *       porque puede disparar llamadas a APIs externas.</li>
 *   <li><b>Resto de endpoints autenticados</b>: 100 req/min por userId.</li>
 * </ul>
 *
 * <p>Los buckets se almacenan en una caché Caffeine con {@code expireAfterAccess(10 min)}
 * para liberar memoria de usuarios inactivos automáticamente sin necesidad de scheduler.</p>
 *
 * <p>Endpoints excluidos del rate limiting:
 * {@code /health}, {@code /swagger-ui}, {@code /v3/api-docs}, {@code /callback/spotify}.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** Permite desactivar el rate limiting en tests o entornos de desarrollo. */
    @Value("${rate.limit.enabled:true}")
    private boolean enabled;

    // ── Límites ──────────────────────────────────────────────────────────────
    private static final int SEARCH_RPM       = 30;
    private static final int AUTH_RPM         = 100;
    private static final int LOGIN_RPM        = 10;
    private static final int REGISTER_RPM     = 5;
    private static final int SPOTIFY_LOGIN_RPM = 10;

    // ── Store de buckets: clave → Bucket, expiración por inactividad ─────────
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        String path   = request.getRequestURI();
        String method = request.getMethod();

        // Excluir endpoints que no necesitan rate limiting
        if (isExcluded(path)) {
            chain.doFilter(request, response);
            return;
        }

        String bucketKey;
        int rpm;

        if ("POST".equalsIgnoreCase(method) && "/v1/auth/login".equals(path)) {
            bucketKey = "ip:login:" + extractIp(request);
            rpm       = LOGIN_RPM;
        } else if ("POST".equalsIgnoreCase(method) && "/v1/users".equals(path)) {
            bucketKey = "ip:register:" + extractIp(request);
            rpm       = REGISTER_RPM;
        } else if ("GET".equalsIgnoreCase(method) && "/v1/auth/spotify".equals(path)) {
            bucketKey = "ip:spotify-login:" + extractIp(request);
            rpm       = SPOTIFY_LOGIN_RPM;
        } else {
            String userId = resolveUserId();
            if (userId == null) {
                // Petición sin autenticar a ruta no pública — JwtAuthFilter/Security la rechazará con 401
                chain.doFilter(request, response);
                return;
            }
            if ("GET".equalsIgnoreCase(method) && path.startsWith("/v1/search")) {
                bucketKey = "user:search:" + userId;
                rpm       = SEARCH_RPM;
            } else {
                bucketKey = "user:general:" + userId;
                rpm       = AUTH_RPM;
            }
        }

        Bucket bucket = buckets.get(bucketKey, k -> buildBucket(rpm));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\":\"RATE_LIMIT\",\"message\":\"Demasiadas peticiones. Inténtalo en un momento.\",\"status\":429}");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private Bucket buildBucket(int requestsPerMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /** Extrae el userId del SecurityContext (establecido por JwtAuthFilter). */
    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String principal) {
            return principal;
        }
        return null;
    }

    /**
     * Obtiene la IP real del cliente.
     * En Railway (y cualquier reverse proxy con forward-headers-strategy=framework),
     * Spring ya resuelve {@code X-Forwarded-For} automáticamente en {@code getRemoteAddr()}.
     */
    private String extractIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /** Endpoints que no necesitan rate limiting. */
    private boolean isExcluded(String path) {
        return path.equals("/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/callback/spotify");
    }
}