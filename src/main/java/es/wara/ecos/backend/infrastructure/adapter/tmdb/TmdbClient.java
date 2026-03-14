package es.wara.ecos.backend.infrastructure.adapter.tmdb;

import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBCreditDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMovieDetailDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBSearchResponseDTO;
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

import java.util.Map;

/**
 * Cliente para interactuar con la API de TMDb (The Movie Database).
 * <p>
 * Proporciona búsqueda de películas, detalles y créditos (director, reparto).
 * Usa autenticación Bearer Token en todos los endpoints.
 * </p>
 * <p>
 * <b>Variables de entorno requeridas:</b> {@code TMDB_API_KEY}, {@code TMDB_API_TOKEN}
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @see <a href="https://www.themoviedb.org/documentation/api">TMDb API Documentation</a>
 * @since 1.0.0
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
     * Valida que las credenciales de TMDb estén configuradas al arrancar la aplicación.
     *
     * @throws IllegalStateException si TMDB_API_KEY o TMDB_API_TOKEN no están definidos
     */
    @PostConstruct
    private void validateCredentials() {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiToken)) {
            log.error("✗ Credenciales de {} no configuradas correctamente", API_NAME);
            throw new IllegalStateException(
                "Las credenciales de TMDB (TMDB_API_KEY y TMDB_API_TOKEN) deben estar configuradas en el archivo .env"
            );
        }
        log.info("Credenciales de {} configuradas correctamente", API_NAME);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BÚSQUEDA Y CRÉDITOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca películas por título en TMDb.
     * <p>
     * Devuelve resultados paginados (20 por página).
     * </p>
     *
     * @param query término de búsqueda (título o parte del título)
     * @param page  número de página (1-based; null = página 1)
     * @return DTO con la lista de películas y datos de paginación
     * @throws IllegalArgumentException si query es vacío
     * @throws ApiRateLimitException    si se excede el límite de peticiones
     * @throws ExternalApiException     para otros errores de la API
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public TMDBSearchResponseDTO searchMovies(String query, Integer page) {
        log.info("Buscando películas con query '{}' en {} API...", query, API_NAME);

        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("El término de búsqueda no puede estar vacío");
        }

        String url = buildUrl("/search/movie", Map.of(
            "query", query,
            "page", page != null ? String.valueOf(page) : "1"
        ));

        return executeTypedRequest(url, TMDBSearchResponseDTO.class, "búsqueda de películas");
    }

    /**
     * Obtiene los créditos (director y reparto) de una película.
     * <p>
     * El director se identifica filtrando el crew por {@code job == "Director"}.
     * </p>
     *
     * @param movieId ID de la película en TMDb
     * @return DTO con cast y crew de la película
     * @throws IllegalArgumentException si movieId es null o negativo
     * @throws ApiRateLimitException    si se excede el límite de peticiones
     * @throws ExternalApiException     para otros errores de la API
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public TMDBCreditDTO getMovieCredits(Long movieId) {
        log.info("Obteniendo créditos de la película ID {} en {} API...", movieId, API_NAME);

        if (movieId == null || movieId <= 0) {
            throw new IllegalArgumentException("El ID de la película debe ser un número positivo");
        }

        String url = buildUrl("/movie/" + movieId + "/credits", null);

        return executeTypedRequest(url, TMDBCreditDTO.class, "créditos de la película");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS AUXILIARES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca series de TV por título.
     *
     * @param query término de búsqueda
     * @param page  número de página (null = 1)
     * @return mapa con resultados de búsqueda
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
     * Obtiene los detalles completos de una película por su ID.
     * <p>
     * A diferencia de {@code searchMovies}, este endpoint devuelve géneros con nombre
     * completo (no solo IDs). Usado en el flujo de agregación cinematográfica.
     * </p>
     *
     * @param movieId ID de la película en TMDb
     * @return DTO con los detalles de la película, incluidos géneros con nombre
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public TMDBMovieDetailDTO getMovieDetails(Long movieId) {
        log.info("Obteniendo detalles de la película ID {} en {} API...", movieId, API_NAME);

        if (movieId == null || movieId <= 0) {
            throw new IllegalArgumentException("El ID de la película debe ser un número positivo");
        }

        return executeTypedRequest(buildUrl("/movie/" + movieId, null), TMDBMovieDetailDTO.class, "detalles de la película");
    }

    /**
     * Obtiene los géneros de películas disponibles en TMDb.
     *
     * @return mapa con la lista de géneros
     */
    public Map<String, Object> getMovieGenres() {
        log.info("Obteniendo géneros de películas en {} API...", API_NAME);
        return executeRequest(buildUrl("/genre/movie/list", null), "géneros de películas");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    /** Construye la URL completa con parámetros adicionales. */
    private String buildUrl(String endpoint, Map<String, String> additionalParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(API_BASE_URL + endpoint)
                .queryParam("api_key", apiKey);

        if (additionalParams != null) {
            additionalParams.forEach(builder::queryParam);
        }

        return builder.toUriString();
    }

    /** Ejecuta una petición GET autenticada y devuelve la respuesta en el tipo indicado. */
    private <T> T executeTypedRequest(String url, Class<T> responseType, String operationDescription) {
        try {
            log.debug("Realizando petición GET a: {}", url.replaceAll("api_key=[^&]+", "api_key=***"));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("✓ {} obtenida exitosamente de {} API", operationDescription, API_NAME);
                return response.getBody();
            } else {
                throw new ExternalApiException(API_NAME, "Respuesta inválida del servidor");
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
            log.error("✗ Error inesperado al consultar {} API: {}", API_NAME, e.getMessage(), e);
            throw new ExternalApiException(API_NAME, "Error inesperado: " + e.getMessage(), e);
        }
    }

    /** Ejecuta una petición GET autenticada y devuelve la respuesta como Map. */
    private Map<String, Object> executeRequest(String url, String operationDescription) {
        try {
            log.debug("Realizando petición GET a: {}", url.replaceAll("api_key=[^&]+", "api_key=***"));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("✓ {} obtenida exitosamente de {} API", operationDescription, API_NAME);
                return response.getBody();
            } else {
                throw new ExternalApiException(API_NAME, "Respuesta inválida del servidor");
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
            log.error("✗ Error inesperado al consultar {} API: {}", API_NAME, e.getMessage(), e);
            throw new ExternalApiException(API_NAME, "Error inesperado: " + e.getMessage(), e);
        }
    }

    /** Maneja errores HTTP 4xx de TMDb. */
    private void handleClientError(HttpClientErrorException e) {
        int statusCode = e.getStatusCode().value();
        log.error("✗ Error del cliente ({}) en {} API: {}", statusCode, API_NAME, e.getMessage());

        switch (statusCode) {
            case 401, 403 -> throw new ApiAuthenticationException(API_NAME, statusCode,
                    "API Key o Token inválido o sin permisos");
            case 429 -> throw new ApiRateLimitException(API_NAME, "Límite de peticiones excedido");
            case 404 -> throw new ExternalApiException(API_NAME, statusCode, "Recurso no encontrado");
            default -> throw new ExternalApiException(API_NAME, statusCode,
                    "Error del cliente: " + e.getMessage(), e);
        }
    }

    /** Maneja errores HTTP 5xx de TMDb. */
    private void handleServerError(HttpServerErrorException e) {
        int statusCode = e.getStatusCode().value();
        log.error("✗ Error del servidor ({}) en {} API: {}", statusCode, API_NAME, e.getMessage());
        throw new ApiServerException(API_NAME, statusCode,
                "El servidor de la API está experimentando problemas", e);
    }
}