package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.LoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Slf4j
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
     * Desvincula la cuenta de Spotify del usuario autenticado.
     * <p>
     * Se rechaza con {@code 409 Conflict} si el usuario no tiene contraseña propia,
     * ya que quedaría sin forma de autenticarse en ECOS.
     * </p>
     *
     * @return {@code 204 No Content} si la desvinculación fue exitosa
     */
    @DeleteMapping("/v1/auth/spotify")
    public ResponseEntity<Void> unlinkSpotify() {
        UUID userId = SecurityUtils.getCurrentUserId();
        spotifyAuthPort.unlinkSpotify(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Callback que Spotify invoca tras la autorización del usuario.
     * <p>
     * <ul>
     *   <li>Flujo <b>LOGIN</b>: genera JWT y redirige a
     *       {@code ecos://auth/callback?token=<jwt>&userId=<id>}</li>
     *   <li>Flujo <b>LINK</b>: persiste el token y redirige a
     *       {@code ecos://auth/callback?linked=true}</li>
     *   <li>Error (p.ej. usuario canceló): devuelve página HTML informativa.</li>
     * </ul>
     * </p>
     *
     * @param code  código de autorización de un solo uso devuelto por Spotify
     * @param error presente cuando el usuario deniega el acceso o Spotify reporta un fallo
     * @param state token anti-CSRF generado al iniciar el flujo
     */
    @GetMapping("/callback/spotify")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state) {

        if (error != null || code == null || state == null) {
            String msg = "access_denied".equals(error)
                    ? "Cancelaste la conexión con Spotify."
                    : "No se recibieron los parámetros esperados de Spotify.";
            return errorPage(msg);
        }

        try {
            Optional<LoginResponse> loginResult = spotifyAuthPort.exchangeCodeForToken(code, state);

            if (loginResult.isPresent()) {
                LoginResponse login = loginResult.get();
                String deepLink = deepLinkScheme + "://auth/callback"
                        + "?token=" + login.token()
                        + "&userId=" + login.userId()
                        + (login.accountLinked() ? "&linked=true" : "");
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(deepLink))
                        .build();
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(deepLinkScheme + "://auth/callback?linked=true"))
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn("Callback Spotify rechazado: {}", e.getMessage());
            return errorPage(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en callback Spotify", e);
            return errorPage("Error interno. Inténtalo de nuevo desde la app.");
        }
    }

    private ResponseEntity<String> errorPage(String reason) {
        String safe = reason == null ? "Error desconocido"
                : reason.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width,initial-scale=1"/>
                  <title>ECOS</title>
                  <style>
                    *{margin:0;padding:0;box-sizing:border-box}
                    body{background:#080B1A;color:#E8ECF4;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;min-height:100vh;display:flex;align-items:center;justify-content:center}
                    .card{text-align:center;padding:48px 32px;max-width:380px;width:90%%;background:#0D1128;border:1px solid #1E2448;border-radius:16px}
                    .logo{font-size:13px;font-weight:700;letter-spacing:4px;color:#22D3EE;text-transform:uppercase;margin-bottom:32px;opacity:.8}
                    .icon{width:56px;height:56px;border-radius:50%%;background:rgba(255,59,48,.12);border:1px solid rgba(255,59,48,.25);display:flex;align-items:center;justify-content:center;margin:0 auto 20px}
                    h1{font-size:18px;font-weight:700;margin-bottom:8px}
                    p{font-size:14px;color:#8A96B8;margin-bottom:20px;line-height:1.6}
                    .detail{background:rgba(255,59,48,.08);border:1px solid rgba(255,59,48,.2);border-radius:8px;padding:10px 14px;font-size:13px;color:#FF6B6B;margin-bottom:16px}
                    .hint{font-size:12px;color:#4A5270}
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="logo">ECOS</div>
                    <div class="icon">
                      <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                        <path d="M18 6L6 18M6 6l12 12" stroke="#FF3B30" stroke-width="2.5" stroke-linecap="round"/>
                      </svg>
                    </div>
                    <h1>No se pudo conectar</h1>
                    <p>Cierra esta ventana e inténtalo de nuevo desde la app.</p>
                    <div class="detail">%s</div>
                    <p class="hint">Si el problema persiste, contacta con soporte.</p>
                  </div>
                </body>
                </html>
                """.formatted(safe);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}