package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.infrastructure.adapter.spotify.SpotifyAuthService;
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

    private final SpotifyAuthService spotifyAuthService;

    public SpotifyAuthController(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    /**
     * Genera la URL de autorización de Spotify para que el usuario vincule su cuenta.
     *
     * @param userId identificador del usuario en ECOS
     * @return JSON con la URL de autorización
     */
    @GetMapping("/v1/auth/spotify/login")
    public ResponseEntity<Map<String, String>> login(@RequestParam Long userId) {
        String url = spotifyAuthService.getAuthorizationUrl(userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Callback que Spotify invoca tras la autorización del usuario.
     * Intercambia el código por tokens y los almacena en memoria.
     *
     * @param code  código de autorización devuelto por Spotify
     * @param state identificador del usuario en ECOS (enviado como state)
     * @return confirmación de vinculación
     */
    @GetMapping("/callback/spotify")
    public ResponseEntity<String> callback(@RequestParam String code, @RequestParam String state) {
        spotifyAuthService.exchangeCodeForToken(code, state);
        return ResponseEntity.ok("Cuenta Spotify vinculada");
    }

}
