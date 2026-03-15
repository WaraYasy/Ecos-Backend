package es.wara.ecos.backend.infrastructure.adapter.spotify;

import es.wara.ecos.backend.domain.port.SpotifyPlaylistPort;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifyAudioFeaturesDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifySearchResponseDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifyTrackDTO;
import es.wara.ecos.backend.infrastructure.exception.ApiAuthenticationException;
import es.wara.ecos.backend.infrastructure.exception.ApiRateLimitException;
import es.wara.ecos.backend.infrastructure.exception.ApiServerException;
import es.wara.ecos.backend.infrastructure.exception.ExternalApiException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Cliente para interactuar con la API de Spotify Web API v1.
 * <p>
 * Gestiona dos flujos de autenticación de OAuth2:
 * <ul>
 *   <li><b>Client Credentials</b>: para endpoints públicos (búsqueda, catálogo). Token
 *       cacheado en memoria con margen de renovación de 5 minutos.</li>
 *   <li><b>Authorization Code</b>: para endpoints de usuario (playlists). Token gestionado
 *       por {@link es.wara.ecos.backend.infrastructure.adapter.spotify.SpotifyAuthService}.</li>
 * </ul>
 * </p>
 *
 * <p><b>Endpoints utilizados (estado API feb-2026):</b></p>
 * <pre>
 * CATÁLOGO (Client Credentials)
 *   GET  /search                        búsqueda de tracks, limit max 10 en Dev Mode
 *   GET  /tracks/{id}                   datos completos de un track
 *   GET  /audio-features/{id}           características de audio de un track
 *
 * USUARIO (Authorization Code)
 *   GET  /me                            perfil del usuario autenticado
 *   POST /me/playlists                  crear playlist en la cuenta del usuario
 *   POST /playlists/{id}/items          añadir items a una playlist  ← /tracks DEPRECATED feb-2026
 *
 * AUTH
 *   POST https://accounts.spotify.com/api/token   obtener/renovar token
 * </pre>
 *
 * <p><b>Nota sobre el cambio feb-2026:</b> Spotify renombró {@code /playlists/{id}/tracks}
 * a {@code /playlists/{id}/items} para soportar tracks, podcasts y futuros tipos de contenido.
 * El endpoint antiguo devuelve 403 sin mensaje de error (no 404), lo que puede confundir.
 * Lo mismo aplica para {@code GET/PUT/DELETE /playlists/{id}/items}.</p>
 *
 * <p><b>Restricciones Development Mode (desde feb-2026):</b></p>
 * <ul>
 *   <li>Máximo 5 usuarios autorizados por app</li>
 *   <li>Requiere cuenta Spotify Premium</li>
 *   <li>Búsqueda limitada a {@code limit=10} (producción admite hasta 50)</li>
 *   <li>Endpoints de playlist siguen disponibles</li>
 * </ul>
 *
 * <p><b>Variables de entorno requeridas:</b> {@code SPOTIFY_CLIENT_ID}, {@code SPOTIFY_CLIENT_SECRET}</p>
 *
 * @author Wara Pacheco
 * @version 1.1
 * @see <a href="https://developer.spotify.com/documentation/web-api">Spotify Web API</a>
 * @since 1.0.0
 */
@Slf4j
@Component
public class SpotifyClient implements SpotifyPlaylistPort {

    private static final String API_NAME = "Spotify";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String API_BASE_URL = "https://api.spotify.com/v1";
    private static final long TOKEN_EXPIRY_MARGIN_SECONDS = 300; // 5 minutos de margen

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.market:ES}")
    private String market;

    /** Límite máximo de resultados por búsqueda. Dev Mode de Spotify admite hasta 10; producción hasta 50. */
    @Value("${spotify.search.max-limit:10}")
    private int searchMaxLimit;

    private final RestTemplate restTemplate;

    // Caché del token OAuth2
    private String cachedToken;
    private Instant tokenExpiresAt;

    public SpotifyClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Valida que las credenciales de Spotify estén configuradas al arrancar la aplicación.
     *
     * @throws IllegalStateException si SPOTIFY_CLIENT_ID o SPOTIFY_CLIENT_SECRET no están definidos
     */
    @PostConstruct
    private void validateCredentials() {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            log.error("Credenciales de Spotify no configuradas correctamente");
            throw new IllegalStateException(
                "Las credenciales de Spotify (SPOTIFY_CLIENT_ID y SPOTIFY_CLIENT_SECRET) deben estar configuradas en el archivo .env"
            );
        }
        log.info("✓ Credenciales de Spotify configuradas correctamente");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API PÚBLICA — BÚSQUEDA Y DATOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca tracks en Spotify que coincidan con la query.
     * <p>
     * Reintenta automáticamente ante rate limit (429) y errores de servidor (5xx)
     * con backoff exponencial de 1s, 2s, 4s.
     * </p>
     *
     * @param query texto de búsqueda (título, artista, álbum o combinación)
     * @param limit número máximo de resultados (1-50; Spotify acepta máx. 50)
     * @return respuesta paginada con los tracks encontrados
     * @throws ApiRateLimitException si se excede el límite de peticiones
     * @throws ApiServerException    si el servidor de Spotify falla
     * @throws ExternalApiException  para otros errores de comunicación
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public SpotifySearchResponseDTO searchTracks(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("La query de búsqueda no puede estar vacía");
        }

        String url = UriComponentsBuilder.fromUriString(API_BASE_URL + "/search")
                .queryParam("q", query)
                .queryParam("type", "track")
                .queryParam("limit", Math.min(limit, searchMaxLimit))
                .queryParam("market", market)
                .build()
                .encode()
                .toUriString();

        log.info("Buscando tracks en {} API: '{}'", API_NAME, query);
        return executeApiRequest(url, SpotifySearchResponseDTO.class, "búsqueda de tracks");
    }

    /**
     * Obtiene las características de audio de un track por su ID de Spotify.
     * <p>
     * Reintenta automáticamente ante rate limit (429) y errores de servidor (5xx).
     * </p>
     *
     * @param trackId ID del track en Spotify
     * @return DTO con las características de audio del track
     * @throws ApiRateLimitException si se excede el límite de peticiones
     * @throws ApiServerException    si el servidor de Spotify falla
     * @throws ExternalApiException  para otros errores de comunicación
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public SpotifyAudioFeaturesDTO getTrackAudioFeatures(String trackId) {
        if (!StringUtils.hasText(trackId)) {
            throw new IllegalArgumentException("El ID del track no puede estar vacío");
        }

        String url = API_BASE_URL + "/audio-features/" + trackId;

        log.info("Obteniendo audio features de {} para trackId: {}", API_NAME, trackId);
        return executeApiRequest(url, SpotifyAudioFeaturesDTO.class, "audio features del track");
    }

    /**
     * Obtiene los datos completos de un track por su ID de Spotify.
     * <p>
     * Reintenta automáticamente ante rate limit (429) y errores de servidor (5xx).
     * </p>
     *
     * @param trackId ID del track en Spotify
     * @return DTO con los datos del track (título, artistas, álbum, preview URL, etc.)
     * @throws ApiRateLimitException si se excede el límite de peticiones
     * @throws ApiServerException    si el servidor de Spotify falla
     * @throws ExternalApiException  para otros errores de comunicación
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public SpotifyTrackDTO getTrack(String trackId) {
        if (!StringUtils.hasText(trackId)) {
            throw new IllegalArgumentException("El ID del track no puede estar vacío");
        }

        String url = API_BASE_URL + "/tracks/" + trackId;

        log.info("Obteniendo track de {} con ID: {}", API_NAME, trackId);
        return executeApiRequest(url, SpotifyTrackDTO.class, "datos del track");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API DE USUARIO — EXPORTACIÓN A SPOTIFY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Obtiene el perfil del usuario autenticado en Spotify y devuelve su Spotify user ID.
     *
     * @param userToken access token del usuario (Authorization Code Flow)
     * @return identificador de usuario en Spotify
     */
    @Override
    @SuppressWarnings("unchecked")
    public String getCurrentUserId(String userToken) {
        java.net.URI uri = java.net.URI.create(API_BASE_URL + "/me");
        log.info("Obteniendo perfil de usuario de {} API", API_NAME);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            if (body == null || !body.containsKey("id")) {
                throw new ExternalApiException(API_NAME, "No se pudo obtener el ID de usuario de Spotify");
            }
            return (String) body.get("id");
        } catch (HttpClientErrorException e) {
            log.error("✗ Error {} al obtener perfil de usuario en {} API: {}",
                    e.getStatusCode().value(), API_NAME, e.getResponseBodyAsString());
            throw new ExternalApiException(API_NAME, e.getStatusCode().value(),
                    "Error al obtener perfil de usuario de Spotify", e);
        }
    }

    /**
     * Crea una playlist en la cuenta del usuario autenticado en Spotify.
     *
     * @param userToken      access token del usuario
     * @param spotifyUserId  identificador del usuario en Spotify
     * @param name           nombre de la playlist
     * @param description    descripción de la playlist
     * @return identificador de la playlist creada en Spotify
     */
    @Override
    @SuppressWarnings("unchecked")
    public String createPlaylist(String userToken, String spotifyUserId, String name, String description) {
        java.net.URI uri = java.net.URI.create(API_BASE_URL + "/me/playlists");
        log.info("Creando playlist en {} para usuario {}", API_NAME, spotifyUserId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "name", name,
                "description", description != null ? description : "",
                "public", false
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            if (body == null || !body.containsKey("id")) {
                throw new ExternalApiException(API_NAME, "No se pudo obtener el ID de la playlist creada en Spotify");
            }
            return (String) body.get("id");
        } catch (HttpClientErrorException e) {
            log.error("✗ Error {} al crear playlist en {} API: {}",
                    e.getStatusCode().value(), API_NAME, e.getResponseBodyAsString());
            throw new ExternalApiException(API_NAME, e.getStatusCode().value(),
                    "Error al crear playlist en Spotify", e);
        }
    }

    /**
     * Añade items a una playlist existente en Spotify.
     * <p>
     * Utiliza {@code POST /playlists/{id}/items} (nuevo endpoint desde feb-2026).
     * El endpoint anterior {@code /playlists/{id}/tracks} está deprecado y devuelve 403.
     * </p>
     *
     * @param userToken           access token del usuario (Authorization Code Flow)
     * @param spotifyPlaylistId   identificador de la playlist en Spotify
     * @param trackUris           lista de URIs con formato {@code spotify:track:{spotifyId}}
     */
    @Override
    public void addTracksToPlaylist(String userToken, String spotifyPlaylistId, java.util.List<String> trackUris) {
        if (trackUris == null || trackUris.isEmpty()) {
            log.warn("No hay tracks para añadir a la playlist de Spotify {}", spotifyPlaylistId);
            return;
        }
        java.net.URI uri = java.net.URI.create(API_BASE_URL + "/playlists/" + spotifyPlaylistId + "/items");
        log.info("Añadiendo {} tracks a playlist {} en {} API", trackUris.size(), spotifyPlaylistId, API_NAME);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of("uris", trackUris);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.exchange(uri, HttpMethod.POST, entity, Map.class);
            log.info("✓ {} tracks añadidos exitosamente a playlist {} en {} API",
                    trackUris.size(), spotifyPlaylistId, API_NAME);
        } catch (HttpClientErrorException e) {
            log.error("✗ Error {} al añadir tracks a playlist {} en {} API: {}",
                    e.getStatusCode().value(), spotifyPlaylistId, API_NAME, e.getResponseBodyAsString());
            throw new ExternalApiException(API_NAME, e.getStatusCode().value(),
                    "Error al añadir tracks a la playlist de Spotify", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTENTICACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Obtiene un access token válido de Spotify, usando caché cuando es posible.
     * <p>
     * Solicita un nuevo token si el cacheado expira en menos de 5 minutos.
     * Los tokens de Spotify duran 1 hora (3600 segundos).
     * </p>
     *
     * @return access token OAuth2 válido
     * @throws ApiAuthenticationException si las credenciales son inválidas
     * @throws ApiRateLimitException      si se excede el límite de peticiones de auth
     * @throws ApiServerException         si el servidor de auth de Spotify falla
     */
    public String getAccessToken() {
        if (isTokenValid()) {
            log.debug("Usando token cacheado (válido hasta: {})", tokenExpiresAt);
            return cachedToken;
        }

        log.info("Solicitando nuevo access token a {} API...", API_NAME);
        return requestNewToken();
    }

    /**
     * Invalida el token en caché para forzar la obtención de uno nuevo.
     * <p>
     * Usar cuando se recibe un 401 inesperado al llamar a la API.
     * </p>
     */
    public void invalidateToken() {
        log.debug("Invalidando token en caché de {}", API_NAME);
        cachedToken = null;
        tokenExpiresAt = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ejecuta una petición GET autenticada a la API de Spotify y devuelve la respuesta tipada.
     * Renueva el token automáticamente si recibe un 401.
     */
    private <T> T executeApiRequest(String url, Class<T> responseType, String operationDescription) {
        java.net.URI uri = java.net.URI.create(url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(getAccessToken());
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, entity, responseType);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("✓ {} obtenidos exitosamente de {} API", operationDescription, API_NAME);
                return response.getBody();
            } else {
                throw new ExternalApiException(API_NAME, "Respuesta inválida del servidor");
            }

        } catch (HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            // Renovar token y reintentar una vez si es 401
            if (statusCode == 401) {
                log.warn("Token inválido (401), renovando y reintentando...");
                invalidateToken();
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(getAccessToken());
                    HttpEntity<?> entity = new HttpEntity<>(headers);
                    ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, entity, responseType);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        return response.getBody();
                    }
                } catch (Exception retryEx) {
                    log.error("✗ Reintento tras 401 fallido: {}", retryEx.getMessage());
                }
            }
            handleClientError(e);
            throw new ExternalApiException(API_NAME, "Error inesperado");
        } catch (HttpServerErrorException e) {
            handleServerError(e);
            throw new ExternalApiException(API_NAME, "Error inesperado");
        } catch (ResourceAccessException e) {
            log.error("✗ Error de conexión con {} API: {}", API_NAME, e.getMessage());
            throw new ExternalApiException(API_NAME, "Error de conexión: timeout o servidor no disponible", e);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("✗ Error inesperado al llamar a {} API: {}", API_NAME, e.getMessage(), e);
            throw new ExternalApiException(API_NAME, "Error inesperado: " + e.getMessage(), e);
        }
    }

    /** Verifica si el token cacheado sigue siendo válido (no expira en los próximos 5 min). */
    private boolean isTokenValid() {
        if (cachedToken == null || tokenExpiresAt == null) {
            return false;
        }
        return Instant.now().plusSeconds(TOKEN_EXPIRY_MARGIN_SECONDS).isBefore(tokenExpiresAt);
    }

    /** Solicita un nuevo token a Spotify usando Client Credentials Flow. */
    private String requestNewToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            String body = "grant_type=client_credentials";
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            log.debug("Realizando petición POST a: {}", TOKEN_URL);
            ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return processTokenResponse(response.getBody());
            } else {
                throw new ExternalApiException(API_NAME, "Respuesta inválida del servidor de autenticación");
            }

        } catch (HttpClientErrorException e) {
            handleClientError(e);
            throw new ExternalApiException(API_NAME, "Error inesperado");
        } catch (HttpServerErrorException e) {
            handleServerError(e);
            throw new ExternalApiException(API_NAME, "Error inesperado");
        } catch (ResourceAccessException e) {
            log.error("✗ Error de conexión con {} API: {}", API_NAME, e.getMessage());
            throw new ExternalApiException(API_NAME, "Error de conexión: timeout o servidor no disponible", e);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("✗ Error inesperado al obtener access token de {}: {}", API_NAME, e.getMessage(), e);
            throw new ExternalApiException(API_NAME, "Error inesperado: " + e.getMessage(), e);
        }
    }

    /** Procesa la respuesta del token y actualiza la caché. */
    private String processTokenResponse(Map<String, Object> responseBody) {
        String token = (String) responseBody.get("access_token");
        Integer expiresIn = (Integer) responseBody.get("expires_in");

        if (token == null) {
            throw new ExternalApiException(API_NAME, "Respuesta sin token");
        }

        cachedToken = token;
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600);

        log.info("✓ Access token obtenido (longitud: {} chars, expira en: {}s)",
                token.length(), expiresIn);
        return token;
    }

    /** Maneja errores HTTP 4xx de Spotify. */
    private void handleClientError(HttpClientErrorException e) {
        int statusCode = e.getStatusCode().value();
        log.error("✗ Error del cliente ({}) en {} API: {}", statusCode, API_NAME, e.getMessage());

        switch (statusCode) {
            case 401, 403 -> throw new ApiAuthenticationException(API_NAME, statusCode,
                    "Credenciales inválidas o sin permisos");
            case 429 -> throw new ApiRateLimitException(API_NAME, "Límite de peticiones excedido");
            default -> throw new ExternalApiException(API_NAME, statusCode,
                    "Error del cliente: " + e.getMessage(), e);
        }
    }

    /** Maneja errores HTTP 5xx de Spotify. */
    private void handleServerError(HttpServerErrorException e) {
        int statusCode = e.getStatusCode().value();
        log.error("✗ Error del servidor ({}) en {} API: {}", statusCode, API_NAME, e.getMessage());
        throw new ApiServerException(API_NAME, statusCode,
                "El servidor de la API está experimentando problemas", e);
    }
}