package es.wara.ecos.backend.infrastructure.adapter.tmdb;

import es.wara.ecos.backend.infrastructure.exception.ApiAuthenticationException;
import es.wara.ecos.backend.infrastructure.exception.ApiRateLimitException;
import es.wara.ecos.backend.infrastructure.exception.ApiServerException;
import es.wara.ecos.backend.infrastructure.exception.ExternalApiException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Cliente para interactuar con la API de TMDB (The Movie Database)
 * Proporciona métodos para consultar información de películas y series:
 * - Búsqueda de películas y series
 * - Detalles de películas/series
 * - Trending (contenido popular)
 * - Géneros
 */
@Slf4j
@Component
public class TmdbClient {

    private static final String API_NAME = "TMDB";
    private static final String API_BASE_URL = "https://api.themoviedb.org/3";

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.token}")
    private String apiToken;

    private final RestTemplate restTemplate;

    public TmdbClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Valida que las credenciales estén configuradas correctamente
     */
    @PostConstruct
    private void validateCredentials() {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiToken)) {
            log.error("✗ Credenciales de {} no configuradas correctamente", API_NAME);
            throw new IllegalStateException(
                "Las credenciales de TMDB (TMDB_API_KEY y TMDB_API_TOKEN) deben estar configuradas en el archivo .env"
            );
        }
        log.info("✓ Credenciales de {} configuradas correctamente", API_NAME);
    }

    /**
     * Busca películas por título
     *
     * @param query Término de búsqueda
     * @param page Número de página (opcional, default: 1)
     * @return Resultados de búsqueda en formato JSON (Map)
     * @throws ExternalApiException si hay error al consultar la API
     */
    public Map<String, Object> searchMovies(String query, Integer page) {
        log.info("Buscando películas con query '{}' en {} API...", query, API_NAME);

        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("El término de búsqueda no puede estar vacío");
        }

        String url = buildUrl("/search/movie", Map.of(
            "query", query,
            "page", page != null ? String.valueOf(page) : "1"
        ));

        return executeRequest(url, "búsqueda de películas");
    }

    /**
     * Busca series de TV por título
     *
     * @param query Término de búsqueda
     * @param page Número de página (opcional, default: 1)
     * @return Resultados de búsqueda en formato JSON (Map)
     * @throws ExternalApiException si hay error al consultar la API
     */
    public Map<String, Object> searchTvShows(String query, Integer page) {
        log.info("Buscando series con query '{}' en {} API...", query, API_NAME);

        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("El término de búsqueda no puede estar vacío");
        }

        String url = buildUrl("/search/tv", Map.of(
            "query", query,
            "page", page != null ? String.valueOf(page) : "1"
        ));

        return executeRequest(url, "búsqueda de series");
    }

    /**
     * Obtiene los detalles de una película por su ID
     *
     * @param movieId ID de la película en TMDB
     * @return Detalles de la película en formato JSON (Map)
     * @throws ExternalApiException si hay error al consultar la API
     */
    public Map<String, Object> getMovieDetails(Long movieId) {
        log.info("Obteniendo detalles de la película ID {} en {} API...", movieId, API_NAME);

        if (movieId == null || movieId <= 0) {
            throw new IllegalArgumentException("El ID de la película debe ser un número positivo");
        }

        String url = buildUrl("/movie/" + movieId, null);

        return executeRequest(url, "detalles de la película");
    }

    /**
     * Obtiene los detalles de una serie por su ID
     *
     * @param tvId ID de la serie en TMDB
     * @return Detalles de la serie en formato JSON (Map)
     * @throws ExternalApiException si hay error al consultar la API
     */
    public Map<String, Object> getTvShowDetails(Long tvId) {
        log.info("Obteniendo detalles de la serie ID {} en {} API...", tvId, API_NAME);

        if (tvId == null || tvId <= 0) {
            throw new IllegalArgumentException("El ID de la serie debe ser un número positivo");
        }

        String url = buildUrl("/tv/" + tvId, null);

        return executeRequest(url, "detalles de la serie");
    }

    /**
     * Obtiene el contenido trending (popular) del día
     *
     * @param mediaType Tipo de contenido: "movie", "tv" o "all"
     * @return Contenido trending en formato JSON (Map)
     * @throws ExternalApiException si hay error al consultar la API
     */
    public Map<String, Object> getTrending(String mediaType) {
        log.info("Obteniendo trending de {} en {} API...", mediaType, API_NAME);

        if (!StringUtils.hasText(mediaType)) {
            mediaType = "all";
        }

        String url = buildUrl("/trending/" + mediaType + "/day", null);

        return executeRequest(url, "trending " + mediaType);
    }

    /**
     * Obtiene la lista de géneros de películas
     *
     * @return Lista de géneros en formato JSON (Map)
     * @throws ExternalApiException si hay error al consultar la API
     */
    public Map<String, Object> getMovieGenres() {
        log.info("Obteniendo géneros de películas en {} API...", API_NAME);

        String url = buildUrl("/genre/movie/list", null);

        return executeRequest(url, "géneros de películas");
    }

    /**
     * Construye la URL completa con los parámetros necesarios
     */
    private String buildUrl(String endpoint, Map<String, String> additionalParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(API_BASE_URL + endpoint)
                .queryParam("api_key", apiKey);

        // Agregar parámetros adicionales
        if (additionalParams != null) {
            additionalParams.forEach(builder::queryParam);
        }

        return builder.toUriString();
    }

    /**
     * Ejecuta la petición HTTP con autenticación Bearer y maneja los errores
     */
    private Map<String, Object> executeRequest(String url, String operationDescription) {
        try {
            log.debug("Realizando petición GET a: {}", url.replaceAll("api_key=[^&]+", "api_key=***"));

            // TMDB prefiere autenticación con Bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

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
            throw e; // Re-lanzar excepciones de API ya manejadas
        } catch (Exception e) {
            log.error("✗ Error inesperado al consultar {} API: {}", API_NAME, e.getMessage(), e);
            throw new ExternalApiException(API_NAME, "Error inesperado: " + e.getMessage(), e);
        }
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
                        "API Key o Token inválido o sin permisos");
            case 429:
                throw new ApiRateLimitException(API_NAME,
                        "Límite de peticiones excedido");
            case 404:
                throw new ExternalApiException(API_NAME, statusCode,
                        "Recurso no encontrado");
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
}