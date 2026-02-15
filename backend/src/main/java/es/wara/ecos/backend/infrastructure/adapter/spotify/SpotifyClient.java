package es.wara.ecos.backend.infrastructure.adapter.spotify;

import es.wara.ecos.backend.infrastructure.exception.ApiAuthenticationException;
import es.wara.ecos.backend.infrastructure.exception.ApiRateLimitException;
import es.wara.ecos.backend.infrastructure.exception.ApiServerException;
import es.wara.ecos.backend.infrastructure.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Cliente para interactuar con la API de Spotify.
 * <p>
 * Este cliente implementa el flujo de autenticación Client Credentials de OAuth2
 * para obtener tokens de acceso que permiten consultar la API de Spotify sin
 * necesidad de autenticación de usuario final.
 * </p>
 * <p>
 * <b>Características principales:</b>
 * <ul>
 *   <li>Autenticación automática mediante Client Credentials Flow</li>
 *   <li>Caché inteligente de tokens (1 hora con margen de 5 minutos)</li>
 *   <li>Validación de credenciales al inicio de la aplicación</li>
 *   <li>Manejo robusto de errores HTTP (401, 403, 429, 5xx)</li>
 *   <li>Logging detallado de todas las operaciones</li>
 *   <li>Timeouts configurados (5s conexión, 10s lectura)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Configuración requerida:</b><br>
 * Las siguientes variables deben estar configuradas en el archivo .env:
 * <ul>
 *   <li><b>SPOTIFY_CLIENT_ID</b>: ID de cliente de Spotify</li>
 *   <li><b>SPOTIFY_CLIENT_SECRET</b>: Secret de cliente de Spotify</li>
 * </ul>
 * </p>
 * <p>
 * <b>Ejemplo de uso:</b>
 * <pre>{@code
 * @Autowired
 * private SpotifyClient spotifyClient;
 *
 * public void ejemplo() {
 *     String token = spotifyClient.getAccessToken();
 *     // Usar el token para llamadas a la API de Spotify
 * }
 * }</pre>
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @see <a href="https://developer.spotify.com/documentation/web-api">Spotify Web API</a>
 * @since 1.0.0
 */
@Slf4j
@Component
public class SpotifyClient {

    private static final String API_NAME = "Spotify";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final long TOKEN_EXPIRY_MARGIN_SECONDS = 300; // 5 minutos de margen

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    // Caché del token
    private String cachedToken;
    private Instant tokenExpiresAt;

    public SpotifyClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Valida que las credenciales de Spotify estén configuradas correctamente.
     * <p>
     * Este método se ejecuta automáticamente después de la construcción del bean
     * para garantizar que la aplicación no inicie sin las credenciales necesarias.
     * </p>
     *
     * @throws IllegalStateException si las credenciales no están configuradas
     */
    @PostConstruct
    private void validateCredentials() {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            log.error("✗ Credenciales de Spotify no configuradas correctamente");
            throw new IllegalStateException(
                "Las credenciales de Spotify (SPOTIFY_CLIENT_ID y SPOTIFY_CLIENT_SECRET) deben estar configuradas en el archivo .env"
            );
        }
        log.info("✓ Credenciales de Spotify configuradas correctamente");
    }

    /**
     * Obtiene un access token válido de Spotify.
     * <p>
     * Este método implementa una estrategia de caché para optimizar las peticiones:
     * <ol>
     *   <li>Verifica si existe un token en caché válido</li>
     *   <li>Si el token es válido (no expira en los próximos 5 minutos), lo retorna</li>
     *   <li>Si no hay token o está próximo a expirar, solicita uno nuevo</li>
     *   <li>Cachea el nuevo token con su tiempo de expiración</li>
     * </ol>
     * </p>
     * <p>
     * Los tokens de Spotify tienen una duración de 1 hora. Este método mantiene
     * un margen de seguridad de 5 minutos para evitar usar tokens a punto de expirar.
     * </p>
     * <p>
     * <b>Manejo de errores:</b><br>
     * Lanza excepciones específicas según el tipo de error:
     * <ul>
     *   <li>{@link ApiAuthenticationException} - Credenciales inválidas (401, 403)</li>
     *   <li>{@link ApiRateLimitException} - Límite de peticiones excedido (429)</li>
     *   <li>{@link ApiServerException} - Error del servidor de Spotify (5xx)</li>
     *   <li>{@link ExternalApiException} - Otros errores (timeout, red, etc.)</li>
     * </ul>
     * </p>
     *
     * @return Access token válido de Spotify (cadena de ~139 caracteres)
     * @throws ApiAuthenticationException si las credenciales son inválidas
     * @throws ApiRateLimitException si se excede el límite de peticiones
     * @throws ApiServerException si hay problemas en el servidor de Spotify
     * @throws ExternalApiException para otros errores de comunicación
     */
    public String getAccessToken() {
        // Verificar si tenemos un token cacheado válido
        if (isTokenValid()) {
            log.debug("Usando token cacheado (válido hasta: {})", tokenExpiresAt);
            return cachedToken;
        }

        log.info("Solicitando nuevo access token a {} API...", API_NAME);
        return requestNewToken();
    }

    /**
     * Verifica si el token en caché aún es válido
     */
    private boolean isTokenValid() {
        if (cachedToken == null || tokenExpiresAt == null) {
            return false;
        }

        // Verificar si el token expira en menos de 5 minutos
        return Instant.now().plusSeconds(TOKEN_EXPIRY_MARGIN_SECONDS).isBefore(tokenExpiresAt);
    }

    /**
     * Solicita un nuevo token a la API de Spotify
     */
    private String requestNewToken() {
        try {
            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Autenticación Basic con credenciales codificadas en Base64
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            String body = "grant_type=client_credentials";

            log.debug("Realizando petición POST a: {}", TOKEN_URL);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return processTokenResponse(response.getBody());
            } else {
                log.error("✗ Respuesta inesperada de {} API. Status: {}", API_NAME, response.getStatusCode());
                throw new ExternalApiException(API_NAME, "Respuesta inválida del servidor");
            }

        } catch (HttpClientErrorException e) {
            handleClientError(e);
            throw new ExternalApiException(API_NAME, "Error inesperado"); // Nunca se alcanza
        } catch (HttpServerErrorException e) {
            handleServerError(e);
            throw new ExternalApiException(API_NAME, "Error inesperado"); // Nunca se alcanza
        } catch (ResourceAccessException e) {
            log.error("✗ Error de conexión con {} API: {}", API_NAME, e.getMessage());
            throw new ExternalApiException(API_NAME, "Error de conexión: timeout o servidor no disponible", e);
        } catch (ExternalApiException e) {
            throw e; // Relanzar excepciones de API ya manejadas
        } catch (Exception e) {
            log.error("✗ Error inesperado al obtener access token de {}: {}", API_NAME, e.getMessage(), e);
            throw new ExternalApiException(API_NAME, "Error inesperado: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa la respuesta del token y actualiza la caché
     */
    private String processTokenResponse(Map<String, Object> responseBody) {
        String token = (String) responseBody.get("access_token");
        Integer expiresIn = (Integer) responseBody.get("expires_in");

        if (token == null) {
            log.error("✗ La respuesta de {} API no contiene access_token", API_NAME);
            throw new ExternalApiException(API_NAME, "Respuesta sin token");
        }

        // Actualizar caché
        cachedToken = token;
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600);

        log.info("✓ Access token obtenido exitosamente (longitud: {} caracteres, expira en: {} segundos)",
                token.length(), expiresIn);
        log.debug("Token: {}... (válido hasta: {})",
                token.substring(0, Math.min(20, token.length())), tokenExpiresAt);

        return token;
    }

    /**
     * Maneja errores 4xx de la API
     */
    private void handleClientError(HttpClientErrorException e) {
        int statusCode = e.getStatusCode().value();

        log.error("✗ Error del cliente ({}) al comunicarse con {} API: {}",
                statusCode, API_NAME, e.getMessage());

        switch (statusCode) {
            case 401:
            case 403:
                throw new ApiAuthenticationException(API_NAME, statusCode,
                        "Credenciales inválidas o sin permisos");
            case 429:
                throw new ApiRateLimitException(API_NAME,
                        "Límite de peticiones excedido");
            default:
                throw new ExternalApiException(API_NAME, statusCode,
                        "Error del cliente: " + e.getMessage(), e);
        }
    }

    /**
     * Maneja errores 5xx de la API
     */
    private void handleServerError(HttpServerErrorException e) {
        int statusCode = e.getStatusCode().value();

        log.error("✗ Error del servidor ({}) en {} API: {}",
                statusCode, API_NAME, e.getMessage());

        throw new ApiServerException(API_NAME, statusCode,
                "El servidor de la API está experimentando problemas", e);
    }

    /**
     * Invalida el token en caché forzando la obtención de uno nuevo.
     * <p>
     * Este método limpia el token almacenado en caché, lo que provocará que
     * la próxima llamada a {@link #getAccessToken()} solicite un token nuevo
     * a la API de Spotify en lugar de usar el cacheado.
     * </p>
     * <p>
     * <b>Casos de uso:</b>
     * <ul>
     *   <li>Renovar el token después de errores de autenticación inesperados</li>
     *   <li>Testing y desarrollo (forzar nueva autenticación)</li>
     *   <li>Actualización de credenciales en tiempo de ejecución</li>
     * </ul>
     * </p>
     */
    public void invalidateToken() {
        log.debug("Invalidando token en caché");
        cachedToken = null;
        tokenExpiresAt = null;
    }
}