package es.wara.ecos.backend.infrastructure.adapter.lastfm;

import es.wara.ecos.backend.infrastructure.exception.ApiAuthenticationException;
import es.wara.ecos.backend.infrastructure.exception.ApiRateLimitException;
import es.wara.ecos.backend.infrastructure.exception.ApiServerException;
import es.wara.ecos.backend.infrastructure.exception.ExternalApiException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Cliente para interactuar con la API de Last.fm.
 * <p>
 * Last.fm es una base de datos musical que proporciona información detallada
 * sobre artistas, canciones, álbumes y estadísticas de escucha. Este cliente
 * facilita la consulta de esta información mediante métodos simples y tipados.
 * </p>
 * <p>
 * <b>Características principales:</b>
 * <ul>
 *   <li>Consulta de información de artistas</li>
 *   <li>Búsqueda de canciones (tracks) con detalles completos</li>
 *   <li>Obtención de top tracks de artistas</li>
 *   <li>Manejo automático de errores específicos de Last.fm</li>
 *   <li>Limpieza de API keys en logs</li>
 *   <li>Validación de credenciales al inicio</li>
 * </ul>
 * </p>
 * <p>
 * <b>Configuración requerida:</b><br>
 * La siguiente variable debe estar configurada en el archivo .env:
 * <ul>
 *   <li><b>LASTFM_API_KEY</b>: API Key de Last.fm</li>
 * </ul>
 * </p>
 * <p>
 * <b>Ejemplo de uso:</b>
 * <pre>{@code
 * @Autowired
 * private LastFmClient lastFmClient;
 *
 * public void ejemplo() {
 *     // Obtener información de un artista
 *     Map<String, Object> artistInfo = lastFmClient.getArtistInfo("Radiohead");
 *
 *     // Obtener top tracks de un artista
 *     Map<String, Object> topTracks = lastFmClient.getArtistTopTracks("The Beatles", 10);
 * }
 * }</pre>
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @see <a href="https://www.last.fm/api">Last.fm API Documentation</a>
 * @since 1.0.0
 */
@Slf4j
@Component
public class LastFmClient {

    private static final String API_NAME = "Last.fm";
    private static final String API_BASE_URL = "https://ws.audioscrobbler.com/2.0/";

    @Value("${lastfm.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public LastFmClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Valida que la API Key de Last.fm esté configurada correctamente.
     * <p>
     * Esta función se ejecuta automáticamente después de la construcción del bean.
     * </p>
     *
     * @throws IllegalStateException si la API Key no está configurada
     */
    @PostConstruct
    private void validateCredentials() {
        if (!StringUtils.hasText(apiKey)) {
            log.error("✗ API Key de {} no configurada correctamente", API_NAME);
            throw new IllegalStateException(
                "La API Key de Last.fm (LASTFM_API_KEY) debe estar configurada en el archivo .env"
            );
        }
        log.info("✓ API Key de {} configurada correctamente", API_NAME);
    }

    /**
     * Obtiene información detallada de un artista por nombre.
     * <p>
     * Consulta la función {@code artist.getInfo} de la API de Last.fm para
     * obtener información completa sobre un artista, incluyendo biografía,
     * estadísticas, imágenes y tags.
     * </p>
     *
     * @param artistName Nombre del artista a buscar (no puede estar vacío)
     * @return Map con información del artista en formato JSON, incluyendo:
     *         <ul>
     *           <li>artist: Objeto con datos del artista</li>
     *           <li>bio: Biografía del artista</li>
     *           <li>stats: Estadísticas (listeners, playcount)</li>
     *           <li>similar: Artistas similares</li>
     *           <li>tags: Tags/géneros asociados</li>
     *         </ul>
     * @throws IllegalArgumentException si artistName es null o vacío
     * @throws ApiAuthenticationException si la API Key es inválida (códigos 10, 26)
     * @throws ApiRateLimitException si se excede el límite de peticiones (código 29)
     * @throws ExternalApiException para otros errores de la API
     */
    public Map<String, Object> getArtistInfo(String artistName) {
        log.info("Consultando información del artista '{}' en {} API...", artistName, API_NAME);

        if (!StringUtils.hasText(artistName)) {
            throw new IllegalArgumentException("El nombre del artista no puede estar vacío");
        }

        String url = buildUrl("artist.getInfo", Map.of("artist", artistName));

        return executeRequest(url, "información del artista");
    }

    /**
     * Busca información de una canción (track)
     *
     * @param trackName Nombre de la canción
     * @param artistName Nombre del artista
     * @return Información de la canción en formato JSON (Map)
     * @throws ExternalApiException si hay error al consultar la API
     */
    public Map<String, Object> getTrackInfo(String trackName, String artistName) {
        log.info("Consultando información del track '{}' de '{}' en {} API...",
                trackName, artistName, API_NAME);

        if (!StringUtils.hasText(trackName) || !StringUtils.hasText(artistName)) {
            throw new IllegalArgumentException("El nombre del track y artista no pueden estar vacíos");
        }

        String url = buildUrl("track.getInfo", Map.of(
            "track", trackName,
            "artist", artistName
        ));

        return executeRequest(url, "información del track");
    }

    /**
     * Obtiene los tracks (canciones más populares) de un artista
     *
     * @param artistName Nombre del artista
     * @param limit Número máximo de resultados (opcional, default: 10)
     * @return Lista de top tracks en formato JSON (Map)
     * @throws ExternalApiException si hay error al consultar la API
     */
    public Map<String, Object> getArtistTopTracks(String artistName, Integer limit) {
        log.info("Consultando top tracks del artista '{}' en {} API...", artistName, API_NAME);

        if (!StringUtils.hasText(artistName)) {
            throw new IllegalArgumentException("El nombre del artista no puede estar vacío");
        }

        Map<String, String> params = new java.util.HashMap<>(Map.of("artist", artistName));
        if (limit != null && limit > 0) {
            params.put("limit", String.valueOf(limit));
        }

        String url = buildUrl("artist.getTopTracks", params);

        return executeRequest(url, "top tracks del artista");
    }

    /**
     * Construye la URL completa con los parámetros necesarios
     */
    private String buildUrl(String method, Map<String, String> additionalParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(API_BASE_URL)
                .queryParam("method", method)
                .queryParam("api_key", apiKey)
                .queryParam("format", "json");

        // Agregar parámetros adicionales
        if (additionalParams != null) {
            additionalParams.forEach(builder::queryParam);
        }

        return builder.toUriString();
    }

    /**
     * Ejecuta la petición HTTP y maneja los errores
     */
    private Map<String, Object> executeRequest(String url, String operationDescription) {
        try {
            log.debug("Realizando petición GET a: {}", url.replaceAll("api_key=[^&]+", "api_key=***"));

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // Last.fm devuelve errores en el body con código 200
                if (body.containsKey("error")) {
                    handleLastFmError(body);
                }

                log.info("✓ {} obtenida exitosamente de {} API", operationDescription, API_NAME);
                log.debug("Respuesta: {} campos en el JSON", body.size());
                return body;
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
            log.error("✗ Error inesperado al consultar {} API: {}", API_NAME, e.getMessage(), e);
            throw new ExternalApiException(API_NAME, "Error inesperado: " + e.getMessage(), e);
        }
    }

    /**
     * Maneja errores específicos de Last.fm que vienen en el body
     */
    private void handleLastFmError(Map<String, Object> body) {
        Integer errorCode = (Integer) body.get("error");
        String errorMessage = (String) body.get("message");

        log.error("✗ Error de {} API - Código: {}, Mensaje: {}", API_NAME, errorCode, errorMessage);

        // Códigos de error de Last.fm: https://www.last.fm/api/errorcodes
        switch (errorCode) {
            case 10: // Invalid API key
            case 26: // Suspended API key
                throw new ApiAuthenticationException(API_NAME, errorMessage);
            case 29: // Rate limit exceeded
                throw new ApiRateLimitException(API_NAME, errorMessage);
            default:
                throw new ExternalApiException(API_NAME, errorCode, errorMessage);
        }
    }

    /**
     * Maneja errores 4xx de la API
     */
    private void handleClientError(HttpClientErrorException e) {
        int statusCode = e.getStatusCode().value();

        log.error("✗ Error del cliente ({}) al comunicarse con {} API: {}",
                statusCode, API_NAME, e.getMessage());

        if (statusCode == HttpStatus.UNAUTHORIZED.value() || statusCode == HttpStatus.FORBIDDEN.value()) {
            throw new ApiAuthenticationException(API_NAME, statusCode, "API Key inválida o sin permisos");
        } else if (statusCode == HttpStatus.TOO_MANY_REQUESTS.value()) {
            throw new ApiRateLimitException(API_NAME, "Límite de peticiones excedido");
        } else {
            throw new ExternalApiException(API_NAME, statusCode, "Error del cliente: " + e.getMessage(), e);
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
}