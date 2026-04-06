package es.wara.ecos.backend.config;

import es.wara.ecos.backend.infrastructure.security.AdminApiKeyFilter;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import es.wara.ecos.backend.infrastructure.security.RateLimitFilter;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security para ECOS: sesión stateless con JWT, rutas públicas y BCrypt.
 *
 * <p><b>Cadena de filtros:</b> {@link JwtAuthFilter} se ejecuta antes de
 * {@code UsernamePasswordAuthenticationFilter}. Extrae el {@code userId} del token JWT
 * y lo registra en el {@code SecurityContextHolder} para que los controladores puedan
 * leerlo vía {@link SecurityUtils}.</p>
 *
 * <p><b>Rutas públicas</b> (sin token requerido):
 * <ul>
 *   <li>{@code POST /v1/auth/login} — autenticación con email y contraseña</li>
 *   <li>{@code POST /v1/users} — registro de nuevos usuarios</li>
 *   <li>{@code GET /callback/spotify} — callback OAuth de Spotify</li>
 *   <li>{@code /swagger-ui.html}, {@code /v3/api-docs/**} — documentación OpenAPI</li>
 *   <li>{@code /health} — health check</li>
 * </ul>
 * Cualquier otra ruta requiere un Bearer token JWT válido; en su ausencia el
 * {@code AuthenticationEntryPoint} devuelve {@code 401} con cuerpo JSON.</p>
 *
 * <p>CSRF está deshabilitado (API stateless). CORS se configura en
 * {@link WebConfig}.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthFilter jwtAuthFilter,
                                                   RateLimitFilter rateLimitFilter,
                                                   AdminApiKeyFilter adminApiKeyFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configure(http))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/users").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/auth/spotify").permitAll()
                .requestMatchers(HttpMethod.GET, "/callback/spotify").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/v1/admin/**").permitAll()   // auth delegada a AdminApiKeyFilter
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"UNAUTHORIZED\",\"message\":\"Token requerido\",\"status\":401}");
                })
            )
            .addFilterBefore(adminApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
