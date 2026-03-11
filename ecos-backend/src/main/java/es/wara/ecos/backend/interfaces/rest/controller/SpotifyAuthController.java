package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador REST para el flujo de autorización de Spotify (Authorization Code Flow).
 * <p>
 * El usuario inicia el flujo en {@code /v1/auth/spotify/login} y Spotify redirige
 * al callback en {@code /callback/spotify}, que coincide con {@code spotify.redirect.uri}.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@RestController
public class SpotifyAuthController {

    private final SpotifyAuthPort spotifyAuthPort;

    /**
     * Inyecta el port de autorización de Spotify.
     *
     * @param spotifyAuthPort adaptador que implementa el flujo OAuth de Spotify
     */
    public SpotifyAuthController(SpotifyAuthPort spotifyAuthPort) {
        this.spotifyAuthPort = spotifyAuthPort;
    }

    /**
     * Inicia el flujo OAuth de Spotify generando la URL de autorización.
     * <p>
     * Genera un {@code state} UUID y lo persiste en la tabla {@code oauth_states} con TTL de 10 min
     * para proteger contra ataques CSRF. El cliente móvil debe redirigir al usuario a la URL devuelta.
     * </p>
     *
     * @return {@code 200 OK} con un JSON {@code {"url": "..."}} apuntando a la pantalla de Spotify
     */
    @GetMapping("/v1/auth/spotify/login")
    public ResponseEntity<Map<String, String>> login() {
        Long userId = SecurityUtils.getCurrentUserId();
        String url = spotifyAuthPort.getAuthorizationUrl(userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Callback que Spotify invoca tras la autorización del usuario.
     * <p>
     * Valida el {@code state} contra la tabla {@code oauth_states} para prevenir CSRF,
     * intercambia el código por tokens de acceso/refresco y los persiste en base de datos.
     * </p>
     *
     * @param code  código de autorización de un solo uso devuelto por Spotify
     * @param state UUID anti-CSRF generado al iniciar el flujo; debe coincidir con el almacenado
     * @return {@code 200 OK} con mensaje de confirmación de vinculación
     */
    @GetMapping("/callback/spotify")
    public ResponseEntity<String> callback(@RequestParam String code, @RequestParam String state) {
        spotifyAuthPort.exchangeCodeForToken(code, state);
        return ResponseEntity.ok("Cuenta Spotify vinculada");
    }

}
