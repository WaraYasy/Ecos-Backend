package es.wara.ecos.backend.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT que intercepta cada petición HTTP, extrae el Bearer token del header
 * {@code Authorization} y, si es válido, establece la autenticación en el
 * {@link SecurityContextHolder}.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * Inyecta el servicio JWT necesario para parsear y validar los tokens.
     *
     * @param jwtService servicio de generación y validación de JWT
     */
    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Intercepta cada petición HTTP exactamente una vez, extrae el Bearer token
     * del header {@code Authorization} y, si es válido, establece la autenticación
     * en el {@link SecurityContextHolder} para que el resto de filtros y controladores
     * puedan acceder al usuario autenticado.
     * <p>
     * Si el header está ausente o el token es inválido/expirado, la petición
     * continúa sin autenticación: las rutas protegidas la rechazarán con 401.
     * </p>
     *
     * @param request     petición HTTP entrante
     * @param response    respuesta HTTP saliente
     * @param filterChain cadena de filtros a continuar tras el procesamiento
     * @throws ServletException si ocurre un error en el procesamiento del servlet
     * @throws IOException      si ocurre un error de E/S durante el filtrado
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Solo procesar si el header tiene el esquema Bearer esperado
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extraer el token descartando el prefijo "Bearer " (7 caracteres)
            String token = authHeader.substring(7);
            try {
                // Parsear el token: lanza excepción si está expirado o la firma es inválida
                Long userId = jwtService.extractUserId(token);

                // Crear el objeto de autenticación con userId como principal; sin credenciales ni roles
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());

                // Registrar la autenticación en el contexto de seguridad del hilo actual
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // Token inválido o expirado — continuar sin autenticación
                // El AuthenticationEntryPoint devolverá 401 si la ruta lo requiere
            }
        }

        // Pasar la petición al siguiente filtro de la cadena
        filterChain.doFilter(request, response);
    }

}
