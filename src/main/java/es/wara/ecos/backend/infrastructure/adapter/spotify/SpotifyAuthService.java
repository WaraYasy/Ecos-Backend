package es.wara.ecos.backend.infrastructure.adapter.spotify;

import es.wara.ecos.backend.application.usecase.user.UserCodeGenerator;
import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
import es.wara.ecos.backend.domain.port.UserRepository;
import es.wara.ecos.backend.infrastructure.exception.ApiServerException;
import es.wara.ecos.backend.infrastructure.persistence.entity.OAuthStateEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SpotifyTokenEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.UserEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaOAuthStateRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSpotifyTokenRepository;
import es.wara.ecos.backend.infrastructure.security.JwtService;
import es.wara.ecos.backend.interfaces.rest.dto.LoginResponse;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del flujo Authorization Code de Spotify.
 * <p>
 * Soporta dos propósitos:
 * <ul>
 *   <li><b>LINK</b>: el usuario ya está autenticado en ECOS y quiere vincular su cuenta de Spotify
 *       para exportar playlists. El state lleva el userId.</li>
 *   <li><b>LOGIN</b>: el usuario quiere autenticarse en ECOS con Spotify. El state no lleva userId.
 *       Tras el callback se llama a {@code /v1/me} de Spotify, se busca o crea el usuario en ECOS
 *       y se devuelve un JWT.</li>
 * </ul>
 * Los tokens se persisten en la tabla {@code spotify_tokens} cifrados con AES-256-GCM.
 * </p>
 *
 * @author Wara Pacheco
 */
@Slf4j
@Service
public class SpotifyAuthService implements SpotifyAuthPort {

    private static final String TOKEN_URL  = "https://accounts.spotify.com/api/token";
    private static final String AUTH_URL   = "https://accounts.spotify.com/authorize";
    private static final String ME_URL     = "https://api.spotify.com/v1/me";
    private static final String SCOPES     = "playlist-modify-public playlist-modify-private user-read-private user-read-email ugc-image-upload";
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
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public SpotifyAuthService(RestTemplate restTemplate,
                              JpaSpotifyTokenRepository tokenRepository,
                              JpaOAuthStateRepository oAuthStateRepository,
                              TextEncryptor encryptor,
                              UserRepository userRepository,
                              JwtService jwtService) {
        this.restTemplate          = restTemplate;
        this.tokenRepository       = tokenRepository;
        this.oAuthStateRepository  = oAuthStateRepository;
        this.encryptor             = encryptor;
        this.userRepository        = userRepository;
        this.jwtService            = jwtService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GENERACIÓN DE URL DE AUTORIZACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public String getAuthorizationUrl(UUID userId) {
        String state = saveState(userId, "LINK");
        return buildAuthUrl(state);
    }

    @Override
    @Transactional
    public String getLoginUrl() {
        String state = saveState(null, "LOGIN");
        return buildAuthUrl(state);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CALLBACK
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Optional<LoginResponse> exchangeCodeForToken(String code, String state) {
        OAuthStateEntity oauthState = oAuthStateRepository.findById(state)
                .orElseThrow(() -> new IllegalArgumentException("Estado OAuth inválido o expirado"));
        if (oauthState.getExpiresAt().isBefore(Instant.now())) {
            oAuthStateRepository.delete(oauthState);
            throw new IllegalArgumentException("Estado OAuth expirado");
        }

        String purpose = oauthState.getPurpose();
        UUID userId    = oauthState.getUserId();
        oAuthStateRepository.delete(oauthState);

        Map<?, ?> tokenBody = fetchTokens(code);

        if ("LOGIN".equals(purpose)) {
            return Optional.of(handleLoginFlow(tokenBody));
        } else {
            persistToken(userId, tokenBody);
            log.info("Token Spotify persistido para userId={}", userId);
            return Optional.empty();
        }
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
    // FLUJO LOGIN — find-or-create usuario
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Llama a /v1/me de Spotify, busca o crea el usuario en ECOS y genera un JWT.
     * <p>
     * Prioridad de búsqueda:
     * <ol>
     *   <li>Por {@code provider_id} (Spotify user ID) → login directo</li>
     *   <li>Por email → vincular la cuenta Spotify al usuario local existente</li>
     *   <li>Sin coincidencia → crear nuevo usuario con auth_provider=spotify</li>
     * </ol>
     * </p>
     */
    private LoginResponse handleLoginFlow(Map<?, ?> tokenBody) {
        String accessToken = (String) tokenBody.get("access_token");
        Map<?, ?> profile  = fetchSpotifyProfile(accessToken);

        String spotifyId    = (String) profile.get("id");
        String email        = (String) profile.get("email");
        String displayName  = (String) profile.get("display_name");

        boolean[] linked = {false};
        User user = userRepository.findByProviderId(spotifyId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existing -> { linked[0] = true; return linkSpotify(existing, spotifyId); })
                        .orElseGet(() -> createSpotifyUser(spotifyId, email, displayName)));

        persistToken(user.getId(), tokenBody);
        log.info("Login con Spotify completado: userId={}, spotifyId={}, linked={}", user.getId(), spotifyId, linked[0]);
        return new LoginResponse(jwtService.generateToken(user.getId()), user.getId(), linked[0]);
    }

    /** Vincula el provider Spotify a una cuenta local existente (merge de cuentas). */
    private User linkSpotify(User user, String spotifyId) {
        log.info("Vinculando Spotify a cuenta existente: userId={}, spotifyId={}", user.getId(), spotifyId);
        user.setProviderId(spotifyId);
        if ("local".equals(user.getAuthProvider())) {
            user.setAuthProvider("spotify+local");
        }
        return userRepository.save(user);
    }

    /** Crea un nuevo usuario de ECOS cuyo único método de login es Spotify. */
    private User createSpotifyUser(String spotifyId, String email, String displayName) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String username = resolveUsername(displayName, email);
            log.info("Creando usuario desde Spotify: spotifyId={}, username={} (intento {})", spotifyId, username, attempt);

            String userCode;
            int codeAttempts = 0;
            do {
                userCode = UserCodeGenerator.generate();
                if (++codeAttempts > 5) throw new IllegalStateException("No se pudo generar un código de usuario único");
            } while (userRepository.existsByUserCode(userCode));

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setAuthProvider("spotify");
            user.setProviderId(spotifyId);
            user.setLanguage("es");
            user.setCreatedAt(LocalDateTime.now());
            user.setUserCode(userCode);
            try {
                return userRepository.save(user);
            } catch (DataIntegrityViolationException e) {
                log.warn("Colisión de username en intento {}: {}", attempt, username);
            }
        }
        throw new IllegalStateException("No se pudo generar un username único para el usuario de Spotify tras 3 intentos");
    }

    /**
     * Genera un username disponible a partir del display_name de Spotify.
     * Si ya está en uso, añade un sufijo numérico aleatorio.
     */
    private String resolveUsername(String displayName, String email) {
        String base = displayName != null && !displayName.isBlank()
                ? displayName.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase()
                : email != null ? email.split("@")[0] : "user";
        if (base.length() < 3) base = base + "ecos";
        if (base.length() > 45) base = base.substring(0, 45);

        String candidate = base;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + (1000 + (int)(Math.random() * 9000));
        }
        return candidate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LLAMADAS A SPOTIFY
    // ─────────────────────────────────────────────────────────────────────────

    private Map<?, ?> fetchTokens(String code) {
        HttpHeaders headers = buildBasicAuthHeaders();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(TOKEN_URL, new HttpEntity<>(body, headers), Map.class);

        Map<?, ?> rb = response.getBody();
        if (rb == null || !rb.containsKey("access_token")) {
            throw new ApiServerException("Spotify", null, "Respuesta inesperada al obtener token");
        }
        return rb;
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> fetchSpotifyProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> response = restTemplate.exchange(
                ME_URL, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);
        Map<?, ?> profile = response.getBody();
        if (profile == null || profile.get("id") == null) {
            throw new ApiServerException("Spotify", null, "Respuesta inesperada al obtener perfil de usuario");
        }
        return profile;
    }

    private SpotifyTokenEntity refreshToken(SpotifyTokenEntity current) {
        if (current.getRefreshToken() == null) {
            log.error("Refresh token ausente en BD para userId={} — el usuario debe reautorizar Spotify",
                    current.getUserId());
            throw new IllegalArgumentException(
                    "Token de Spotify expirado. Por favor vincula tu cuenta de Spotify de nuevo.");
        }
        String refreshToken = encryptor.decrypt(current.getRefreshToken());

        HttpHeaders headers = buildBasicAuthHeaders();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(TOKEN_URL, new HttpEntity<>(body, headers), Map.class);

        Map<?, ?> rb = response.getBody();
        if (rb == null || !rb.containsKey("access_token")) {
            throw new ApiServerException("Spotify", null, "Respuesta inesperada al refrescar token");
        }

        String newAccess  = (String)  rb.get("access_token");
        Integer expiresIn = (Integer) rb.get("expires_in");
        String newRefresh = rb.containsKey("refresh_token")
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

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String saveState(UUID userId, String purpose) {
        String state = UUID.randomUUID().toString();
        OAuthStateEntity entity = new OAuthStateEntity();
        entity.setState(state);
        entity.setUserId(userId);
        entity.setPurpose(purpose);
        entity.setExpiresAt(Instant.now().plusSeconds(600));
        oAuthStateRepository.save(entity);
        return state;
    }

    private String buildAuthUrl(String state) {
        return AUTH_URL
                + "?client_id="     + clientId
                + "&response_type=code"
                + "&redirect_uri="  + redirectUri
                + "&scope="         + SCOPES.replace(" ", "%20")
                + "&state="         + state
                + "&show_dialog=true";
    }

    private void persistToken(UUID userId, Map<?, ?> body) {
        if (body == null || !body.containsKey("access_token")) {
            throw new ApiServerException("Spotify", null, "Respuesta inesperada al persistir token");
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