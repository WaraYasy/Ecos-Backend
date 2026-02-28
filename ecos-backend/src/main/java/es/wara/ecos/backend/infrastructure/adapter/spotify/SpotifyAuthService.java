package es.wara.ecos.backend.infrastructure.adapter.spotify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para el flujo Authorization Code de Spotify.
 * <p>
 * Gestiona tokens de usuario (no de aplicación): obtiene la URL de autorización,
 * intercambia el código por tokens y los mantiene en memoria con soporte de refresco.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@Service
public class SpotifyAuthService {

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String SCOPES = "playlist-modify-public playlist-modify-private user-read-private";

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.redirect.uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<Long, SpotifyUserToken> tokenStore = new ConcurrentHashMap<>();

    private record SpotifyUserToken(String accessToken, String refreshToken, Instant expiresAt) {}

    public SpotifyAuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Genera la URL de autorización de Spotify para que el usuario conceda permisos.
     *
     * @param userId identificador del usuario en ECOS (se usa como state para vincularlo al callback)
     * @return URL completa de autorización de Spotify
     */
    public String getAuthorizationUrl(Long userId) {
        return AUTH_URL
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&redirect_uri=" + redirectUri
                + "&scope=" + SCOPES.replace(" ", "%20")
                + "&state=" + userId;
    }

    /**
     * Intercambia el código de autorización por tokens de acceso y refresco.
     *
     * @param code  código de autorización recibido en el callback
     * @param state identificador del usuario en ECOS (enviado como state)
     * @throws IllegalArgumentException si la respuesta de Spotify no contiene el token
     */
    public void exchangeCodeForToken(String code, String state) {
        Long userId = Long.parseLong(state);

        HttpHeaders headers = buildBasicAuthHeaders();
        String body = "grant_type=authorization_code"
                + "&code=" + code
                + "&redirect_uri=" + redirectUri;

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

        storeTokenFromResponse(userId, response.getBody());
        log.info("Token de usuario Spotify almacenado para userId={}", userId);
    }

    /**
     * Devuelve el access token vigente de un usuario.
     * Refresca automáticamente si está a punto de expirar.
     *
     * @param userId identificador del usuario
     * @return access token de Spotify
     * @throws IllegalArgumentException si el usuario no ha vinculado su cuenta de Spotify
     */
    public String getTokenForUser(Long userId) {
        SpotifyUserToken token = tokenStore.get(userId);
        if (token == null) {
            throw new IllegalArgumentException("El usuario " + userId + " no ha vinculado su cuenta de Spotify");
        }
        if (Instant.now().plusSeconds(300).isAfter(token.expiresAt())) {
            refreshTokenForUser(userId);
            token = tokenStore.get(userId);
        }
        return token.accessToken();
    }

    /**
     * Refresca el access token de un usuario usando su refresh token.
     *
     * @param userId identificador del usuario
     * @throws IllegalArgumentException si el usuario no tiene refresh token almacenado
     */
    public void refreshTokenForUser(Long userId) {
        SpotifyUserToken current = tokenStore.get(userId);
        if (current == null || current.refreshToken() == null) {
            throw new IllegalArgumentException("No hay refresh token disponible para userId=" + userId);
        }

        HttpHeaders headers = buildBasicAuthHeaders();
        String body = "grant_type=refresh_token&refresh_token=" + current.refreshToken();

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

        Map<?, ?> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("access_token")) {
            throw new IllegalArgumentException("Respuesta inválida al refrescar token para userId=" + userId);
        }

        String newAccessToken = (String) responseBody.get("access_token");
        Integer expiresIn = (Integer) responseBody.get("expires_in");
        String newRefreshToken = responseBody.containsKey("refresh_token")
                ? (String) responseBody.get("refresh_token")
                : current.refreshToken();

        tokenStore.put(userId, new SpotifyUserToken(
                newAccessToken,
                newRefreshToken,
                Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600)
        ));
        log.info("Token de Spotify refrescado para userId={}", userId);
    }

    private HttpHeaders buildBasicAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String encoded = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        headers.set("Authorization", "Basic " + encoded);
        return headers;
    }

    private void storeTokenFromResponse(Long userId, Map<?, ?> responseBody) {
        if (responseBody == null || !responseBody.containsKey("access_token")) {
            throw new IllegalArgumentException("Respuesta inválida de Spotify al obtener token para userId=" + userId);
        }
        String accessToken = (String) responseBody.get("access_token");
        String refreshToken = (String) responseBody.get("refresh_token");
        Integer expiresIn = (Integer) responseBody.get("expires_in");

        tokenStore.put(userId, new SpotifyUserToken(
                accessToken,
                refreshToken,
                Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600)
        ));
    }

}
