package es.wara.ecos.backend.infrastructure.adapter.spotify;

import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
import es.wara.ecos.backend.infrastructure.persistence.entity.OAuthStateEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SpotifyTokenEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.UserEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaOAuthStateRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSpotifyTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación del flujo Authorization Code de Spotify.
 * <p>
 * Los tokens se persisten en la tabla {@code spotify_tokens} cifrados con AES-256-GCM,
 * sobreviviendo reinicios y redeploys. El refresco automático mantiene el token vigente
 * sin que el caller tenga que gestionarlo.
 * </p>
 *
 * @author Wara Pacheco
 */
@Slf4j
@Service
public class SpotifyAuthService implements SpotifyAuthPort {

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String AUTH_URL  = "https://accounts.spotify.com/authorize";
    private static final String SCOPES    = "playlist-modify-public playlist-modify-private user-read-private";
    private static final long   REFRESH_MARGIN_SECONDS = 300;

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.redirect.uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final JpaSpotifyTokenRepository tokenRepository;
    private final JpaOAuthStateRepository oAuthStateRepository;
    private final TextEncryptor encryptor;

    public SpotifyAuthService(RestTemplate restTemplate,
                              JpaSpotifyTokenRepository tokenRepository,
                              JpaOAuthStateRepository oAuthStateRepository,
                              TextEncryptor encryptor) {
        this.restTemplate        = restTemplate;
        this.tokenRepository     = tokenRepository;
        this.oAuthStateRepository = oAuthStateRepository;
        this.encryptor           = encryptor;
    }

    @Override
    @Transactional
    public String getAuthorizationUrl(UUID userId) {
        String state = UUID.randomUUID().toString();
        OAuthStateEntity oauthState = new OAuthStateEntity();
        oauthState.setState(state);
        oauthState.setUserId(userId);
        oauthState.setExpiresAt(Instant.now().plusSeconds(600));
        oAuthStateRepository.save(oauthState);

        return AUTH_URL
                + "?client_id="     + clientId
                + "&response_type=code"
                + "&redirect_uri="  + redirectUri
                + "&scope="         + SCOPES.replace(" ", "%20")
                + "&state="         + state;
    }

    @Override
    @Transactional
    public void exchangeCodeForToken(String code, String state) {
        OAuthStateEntity oauthState = oAuthStateRepository.findById(state)
                .orElseThrow(() -> new IllegalArgumentException("Estado OAuth inválido o expirado"));
        if (oauthState.getExpiresAt().isBefore(Instant.now())) {
            oAuthStateRepository.delete(oauthState);
            throw new IllegalArgumentException("Estado OAuth expirado");
        }
        UUID userId = oauthState.getUserId();
        oAuthStateRepository.delete(oauthState);

        HttpHeaders headers = buildBasicAuthHeaders();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(TOKEN_URL, new HttpEntity<>(body, headers), Map.class);

        persistToken(userId, response.getBody());
        log.info("Token Spotify persistido para userId={}", userId);
    }

    @Override
    @Transactional
    public String getTokenForUser(UUID userId) {
        SpotifyTokenEntity token = tokenRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario " + userId + " no ha vinculado su cuenta de Spotify"));

        if (Instant.now().plusSeconds(REFRESH_MARGIN_SECONDS).isAfter(token.getExpiresAt())) {
            token = refreshToken(token);
        }

        return encryptor.decrypt(token.getAccessToken());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Privados
    // ─────────────────────────────────────────────────────────────────────────

    private SpotifyTokenEntity refreshToken(SpotifyTokenEntity current) {
        String refreshToken = encryptor.decrypt(current.getRefreshToken());

        HttpHeaders headers = buildBasicAuthHeaders();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(TOKEN_URL, new HttpEntity<>(body, headers), Map.class);

        Map<?, ?> rb = response.getBody();
        if (rb == null || !rb.containsKey("access_token")) {
            throw new IllegalArgumentException(
                    "Respuesta inválida al refrescar token para userId=" + current.getUserId());
        }

        String newAccess   = (String)  rb.get("access_token");
        Integer expiresIn  = (Integer) rb.get("expires_in");
        String newRefresh  = rb.containsKey("refresh_token")
                ? (String) rb.get("refresh_token")
                : refreshToken;

        current.setAccessToken(encryptor.encrypt(newAccess));
        current.setRefreshToken(encryptor.encrypt(newRefresh));
        current.setExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
        current.setUpdatedAt(Instant.now());

        SpotifyTokenEntity saved = tokenRepository.save(current);
        log.info("Token Spotify refrescado para userId={}", current.getUserId());
        return saved;
    }

    private void persistToken(UUID userId, Map<?, ?> body) {
        if (body == null || !body.containsKey("access_token")) {
            throw new IllegalArgumentException(
                    "Respuesta inválida de Spotify al obtener token para userId=" + userId);
        }

        String accessToken  = (String)  body.get("access_token");
        String refreshToken = (String)  body.get("refresh_token");
        Integer expiresIn   = (Integer) body.get("expires_in");
        String scope        = (String)  body.get("scope");

        SpotifyTokenEntity entity = tokenRepository.findById(userId)
                .orElseGet(() -> {
                    SpotifyTokenEntity e = new SpotifyTokenEntity();
                    UserEntity user = new UserEntity();
                    user.setId(userId);
                    e.setUser(user);
                    e.setCreatedAt(Instant.now());
                    return e;
                });

        entity.setAccessToken(encryptor.encrypt(accessToken));
        entity.setRefreshToken(refreshToken != null ? encryptor.encrypt(refreshToken) : null);
        entity.setExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
        entity.setScope(scope);
        entity.setUpdatedAt(Instant.now());

        tokenRepository.save(entity);
    }

    private HttpHeaders buildBasicAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String encoded = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());
        headers.set("Authorization", "Basic " + encoded);
        return headers;
    }
}
