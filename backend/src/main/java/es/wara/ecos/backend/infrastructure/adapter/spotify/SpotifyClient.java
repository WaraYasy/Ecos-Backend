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
 * Cliente para interactuar con la API de Spotify
 * Maneja autenticación mediante Client Credentials Flow
 * y cachea el token para evitar peticiones innecesarias
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
     * Valida que las credenciales estén configuradas correctamente
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
     * Obtiene un access token válido de Spotify
     * Usa token cacheado si aún es válido, de lo contrario solicita uno nuevo
     *
     * @return Access token válido
     * @throws ExternalApiException si hay error al obtener el token
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
            throw e; // Re-lanzar excepciones de API ya manejadas
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
     * Invalida el token en caché
     * Útil para forzar la obtención de un nuevo token
     */
    public void invalidateToken() {
        log.debug("Invalidando token en caché");
        cachedToken = null;
        tokenExpiresAt = null;
    }
}