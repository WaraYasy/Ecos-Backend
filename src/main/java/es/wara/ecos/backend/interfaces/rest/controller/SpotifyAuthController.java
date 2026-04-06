package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.LoginResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controlador REST para el flujo de autorización de Spotify (Authorization Code Flow).
 *
 * <p>Soporta dos propósitos:</p>
 * <ul>
 *   <li><b>LINK</b> ({@code GET /v1/auth/spotify/login} 🔒): el usuario autenticado vincula
 *       su cuenta de Spotify para poder exportar playlists.</li>
 *   <li><b>LOGIN</b> ({@code GET /v1/auth/spotify} público): el usuario se autentica en ECOS
 *       con su cuenta de Spotify. El callback redirige al deep link {@code ecos://} con el JWT.</li>
 * </ul>
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
@Validated
@RestController
public class SpotifyAuthController {

    @Value("${app.deep-link-scheme:ecos}")
    private String deepLinkScheme;

    private final SpotifyAuthPort spotifyAuthPort;

    public SpotifyAuthController(SpotifyAuthPort spotifyAuthPort) {
        this.spotifyAuthPort = spotifyAuthPort;
    }

    /**
     * Inicia el flujo OAuth de Spotify para <b>login</b> (público, sin JWT requerido).
     * El cliente móvil redirige al usuario a la URL devuelta.
     *
     * @return {@code 200 OK} con {@code {"url": "..."}}
     */
    @GetMapping("/v1/auth/spotify")
    public ResponseEntity<Map<String, String>> loginWithSpotify() {
        String url = spotifyAuthPort.getLoginUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Inicia el flujo OAuth de Spotify para <b>vincular cuenta</b> (requiere JWT).
     * El usuario ya está autenticado en ECOS; este endpoint asocia su Spotify.
     *
     * @return {@code 200 OK} con {@code {"url": "..."}}
     */
    @GetMapping("/v1/auth/spotify/login")
    public ResponseEntity<Map<String, String>> linkSpotify() {
        UUID userId = SecurityUtils.getCurrentUserId();
        String url = spotifyAuthPort.getAuthorizationUrl(userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Callback que Spotify invoca tras la autorización del usuario.
     * <p>
     * <ul>
     *   <li>Flujo <b>LOGIN</b>: genera JWT y redirige a
     *       {@code ecos://auth/callback?token=<jwt>&userId=<id>}</li>
     *   <li>Flujo <b>LINK</b>: persiste el token y devuelve {@code 200 OK}</li>
     * </ul>
     * </p>
     *
     * @param code  código de autorización de un solo uso devuelto por Spotify
     * @param state token anti-CSRF generado al iniciar el flujo
     */
    @GetMapping("/callback/spotify")
    public ResponseEntity<?> callback(
            @RequestParam @NotBlank String code,
            @RequestParam @NotBlank String state) {

        Optional<LoginResponse> loginResult = spotifyAuthPort.exchangeCodeForToken(code, state);

        if (loginResult.isPresent()) {
            LoginResponse login = loginResult.get();
            String deepLink = deepLinkScheme + "://auth/callback"
                    + "?token=" + login.token()
                    + "&userId=" + login.userId();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(deepLink))
                    .build();
        }

        return ResponseEntity.ok("Cuenta Spotify vinculada");
    }
}