package es.wara.ecos.backend.infrastructure.adapter.lastfm;

import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmSimilarTracksDTO;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmTagDTO;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmTagTrackDTO;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmTrackInfoDTO;
import es.wara.ecos.backend.infrastructure.exception.ApiAuthenticationException;
import es.wara.ecos.backend.infrastructure.exception.ApiRateLimitException;
import es.wara.ecos.backend.infrastructure.exception.ApiServerException;
import es.wara.ecos.backend.infrastructure.exception.ExternalApiException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente para interactuar con la API de Last.fm.
 * <p>
 * Proporciona información sobre canciones (tags, similares) y artistas.
 * Last.fm devuelve errores de negocio en el body con HTTP 200, por lo que
 * cada respuesta se comprueba antes de devolver los datos.
 * </p>
 * <p>
 * <b>Variable de entorno requerida:</b> {@code LASTFM_API_KEY}
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
     * Valida que la API Key de Last.fm esté configurada al arrancar la aplicación.
     *
     * @throws IllegalStateException si LASTFM_API_KEY no está definida
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

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRINCIPALES — TAGS Y SIMILARES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Obtiene los top tags (géneros, moods, estéticas) de una canción.
     * <p>
     * Llama a {@code track.getTopTags} de Last.fm. Los tags representan
     * géneros comunitarios como "rock", "dreamy", "cyberpunk", "cottagecore", etc.
     * Si la canción no existe en Last.fm devuelve un DTO con listas vacías.
     * </p>
     *
     * @param artist nombre del artista
     * @param title  título de la canción
     * @return DTO con los top tags; vacío si no hay datos
     * @throws ApiRateLimitException si se excede el límite de peticiones
     * @throws ApiServerException    si el servidor de Last.fm falla
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public LastFmTrackInfoDTO getTrackTags(String artist, String title) {
        if (!StringUtils.hasText(artist) || !StringUtils.hasText(title)) {
            throw new IllegalArgumentException("El artista y título no pueden estar vacíos");
        }

        log.info("Obteniendo tags de '{} - {}' en {} API", artist, title, API_NAME);

        String url = buildUrl("track.getTopTags", Map.of(
            "artist", artist,
            "track", title,
            "autocorrect", "1"
        ));

        Map<String, Object> response = executeRequest(url, "tags del track");
        return parseTrackTags(response);
    }

    /**
     * Obtiene tracks similares a la canción indicada según Last.fm.
     * <p>
     * Llama a {@code track.getSimilar}. La puntuación de similitud ({@code match})
     * va de 0.0 a 1.0. Si la canción no existe devuelve un DTO vacío.
     * </p>
     *
     * @param artist nombre del artista de referencia
     * @param title  título de la canción de referencia
     * @param limit  número máximo de similares a devolver (máx. 100)
     * @return DTO con la lista de tracks similares; vacío si no hay datos
     * @throws ApiRateLimitException si se excede el límite de peticiones
     * @throws ApiServerException    si el servidor de Last.fm falla
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public LastFmSimilarTracksDTO getSimilarTracks(String artist, String title, int limit) {
        if (!StringUtils.hasText(artist) || !StringUtils.hasText(title)) {
            throw new IllegalArgumentException("El artista y título no pueden estar vacíos");
        }

        log.info("Obteniendo tracks similares a '{} - {}' en {} API", artist, title, API_NAME);

        Map<String, String> params = new HashMap<>(Map.of(
            "artist", artist,
            "track", title,
            "autocorrect", "1"
        ));
        if (limit > 0) {
            params.put("limit", String.valueOf(Math.min(limit, 100)));
        }

        String url = buildUrl("track.getSimilar", params);
        Map<String, Object> response = executeRequest(url, "tracks similares");
        return parseSimilarTracks(response);
    }

    /**
     * Obtiene las canciones más populares para un tag (estética/género) dado.
     * <p>
     * Llama a {@code tag.getTopTracks} de Last.fm. Es la pieza central del
     * cuadrante de Estética del sistema de búsqueda: dado un tag comunitario
     * como "cyberpunk", "dreamy" o "cottagecore", devuelve las canciones que
     * la comunidad más asocia con esa estética.
     * </p>
     * <p>
     * Si el tag no existe en Last.fm devuelve un DTO con lista vacía (no lanza
     * excepción). Si el tag tiene pocos tracks, devuelve todos los disponibles.
     * </p>
     *
     * @param tag   nombre del tag / estética (ej. "cyberpunk", "dreamy")
     * @param limit número máximo de canciones a devolver (1-50; 0 = default de Last.fm)
     * @return DTO con la lista de tracks del tag; vacío si el tag no existe
     * @throws IllegalArgumentException si el tag está vacío
     * @throws ApiRateLimitException    si se excede el límite de peticiones
     * @throws ApiServerException       si el servidor de Last.fm falla
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public LastFmTagTrackDTO getTopTracksByTag(String tag, int limit) {
        if (!StringUtils.hasText(tag)) {
            throw new IllegalArgumentException("El tag no puede estar vacío");
        }

        log.info("Obteniendo top tracks del tag '{}' en {} API", tag, API_NAME);

        Map<String, String> params = new HashMap<>(Map.of("tag", tag));
        if (limit > 0) {
            params.put("limit", String.valueOf(Math.min(limit, 50)));
        }

        String url = buildUrl("tag.getTopTracks", params);

        try {
            Map<String, Object> response = executeRequest(url, "top tracks del tag");
            return parseTagTracks(response);
        } catch (ExternalApiException e) {
            // Tag no encontrado (error 6) → devolver lista vacía sin propagar
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                log.warn("Tag '{}' no encontrado en {}, devolviendo lista vacía", tag, API_NAME);
                return new LastFmTagTrackDTO();
            }
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS AUXILIARES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Obtiene información detallada de un artista (bio, estadísticas, tags).
     *
     * @param artistName nombre del artista
     * @return mapa con la información del artista en formato JSON
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
     * Obtiene información de una canción por artista y título.
     *
     * @param trackName  nombre de la canción
     * @param artistName nombre del artista
     * @return mapa con la información de la canción en formato JSON
     */
    public Map<String, Object> getTrackInfo(String trackName, String artistName) {
        log.info("Consultando información del track '{} - {}' en {} API...", trackName, artistName, API_NAME);

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
     * Obtiene los top tracks de un artista.
     *
     * @param artistName nombre del artista
     * @param limit      número máximo de resultados (null = default de Last.fm)
     * @return mapa con los top tracks del artista
     */
    public Map<String, Object> getArtistTopTracks(String artistName, Integer limit) {
        log.info("Consultando top tracks del artista '{}' en {} API...", artistName, API_NAME);

        if (!StringUtils.hasText(artistName)) {
            throw new IllegalArgumentException("El nombre del artista no puede estar vacío");
        }

        Map<String, String> params = new HashMap<>(Map.of("artist", artistName));
        if (limit != null && limit > 0) {
            params.put("limit", String.valueOf(limit));
        }

        String url = buildUrl("artist.getTopTracks", params);
        return executeRequest(url, "top tracks del artista");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSERS — Map → DTO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Convierte la respuesta de {@code track.getTopTags} (Map) al DTO tipado.
     * Estructura esperada: {@code { "toptags": { "tag": [...] } }}
     */
    @SuppressWarnings("unchecked")
    private LastFmTrackInfoDTO parseTrackTags(Map<String, Object> response) {
        LastFmTrackInfoDTO result = new LastFmTrackInfoDTO();

        Object toptagsObj = response.get("toptags");
        if (!(toptagsObj instanceof Map)) {
            return result;
        }

        Map<String, Object> toptags = (Map<String, Object>) toptagsObj;
        Object tagListObj = toptags.get("tag");
        if (!(tagListObj instanceof List)) {
            return result;
        }

        List<Map<String, Object>> tagList = (List<Map<String, Object>>) tagListObj;
        List<LastFmTagDTO> tags = new ArrayList<>();
        for (Map<String, Object> t : tagList) {
            LastFmTagDTO dto = new LastFmTagDTO();
            dto.setName(t.get("name") instanceof String name ? name : null);
            dto.setCount(parseCount(t.get("count")));
            dto.setUrl(t.get("url") instanceof String url ? url : null);
            tags.add(dto);
        }

        LastFmTrackInfoDTO.TopTagsWrapper wrapper = new LastFmTrackInfoDTO.TopTagsWrapper();
        wrapper.setTag(tags);
        result.setToptags(wrapper);
        return result;
    }

    /**
     * Convierte la respuesta de {@code track.getSimilar} (Map) al DTO tipado.
     * Estructura esperada: {@code { "similartracks": { "track": [...] } }}
     */
    @SuppressWarnings("unchecked")
    private LastFmSimilarTracksDTO parseSimilarTracks(Map<String, Object> response) {
        LastFmSimilarTracksDTO result = new LastFmSimilarTracksDTO();

        Object similarObj = response.get("similartracks");
        if (!(similarObj instanceof Map)) {
            return result;
        }

        Map<String, Object> similartracks = (Map<String, Object>) similarObj;
        Object trackListObj = similartracks.get("track");
        if (!(trackListObj instanceof List)) {
            return result;
        }

        List<Map<String, Object>> trackList = (List<Map<String, Object>>) trackListObj;
        List<LastFmSimilarTracksDTO.SimilarTrack> tracks = new ArrayList<>();
        for (Map<String, Object> t : trackList) {
            LastFmSimilarTracksDTO.SimilarTrack track = new LastFmSimilarTracksDTO.SimilarTrack();
            track.setName(t.get("name") instanceof String name ? name : null);
            track.setMatch(parseDouble(t.get("match")));
            track.setUrl(t.get("url") instanceof String url ? url : null);

            // El artista es un objeto anidado: { "name": "..." }
            if (t.get("artist") instanceof Map<?, ?> artistMap) {
                LastFmSimilarTracksDTO.SimilarArtist artist = new LastFmSimilarTracksDTO.SimilarArtist();
                artist.setName(artistMap.get("name") instanceof String name ? name : null);
                track.setArtist(artist);
            }

            tracks.add(track);
        }

        LastFmSimilarTracksDTO.SimilarTracksWrapper wrapper = new LastFmSimilarTracksDTO.SimilarTracksWrapper();
        wrapper.setTrack(tracks);
        result.setSimilartracks(wrapper);
        return result;
    }

    /**
     * Convierte la respuesta de {@code tag.getTopTracks} (Map) al DTO tipado.
     * Estructura esperada: {@code { "tracks": { "track": [...] } }}
     */
    @SuppressWarnings("unchecked")
    private LastFmTagTrackDTO parseTagTracks(Map<String, Object> response) {
        LastFmTagTrackDTO result = new LastFmTagTrackDTO();

        Object tracksObj = response.get("tracks");
        if (!(tracksObj instanceof Map)) {
            return result;
        }

        Map<String, Object> tracks = (Map<String, Object>) tracksObj;
        Object trackListObj = tracks.get("track");
        if (!(trackListObj instanceof List)) {
            return result;
        }

        List<Map<String, Object>> trackList = (List<Map<String, Object>>) trackListObj;
        List<LastFmTagTrackDTO.TagTrack> tagTracks = new ArrayList<>();
        for (Map<String, Object> t : trackList) {
            LastFmTagTrackDTO.TagTrack track = new LastFmTagTrackDTO.TagTrack();
            track.setName(t.get("name") instanceof String name ? name : null);
            track.setDuration(t.get("duration") instanceof String dur ? dur : null);
            track.setUrl(t.get("url") instanceof String url ? url : null);

            if (t.get("artist") instanceof Map<?, ?> artistMap) {
                LastFmTagTrackDTO.TagTrackArtist artist = new LastFmTagTrackDTO.TagTrackArtist();
                artist.setName(artistMap.get("name") instanceof String name ? name : null);
                track.setArtist(artist);
            }

            tagTracks.add(track);
        }

        LastFmTagTrackDTO.TracksWrapper wrapper = new LastFmTagTrackDTO.TracksWrapper();
        wrapper.setTrack(tagTracks);
        result.setTracks(wrapper);
        return result;
    }

    /** Parsea un valor numérico de count desde el Map de Last.fm (puede ser Integer o String). */
    private Integer parseCount(Object value) {
        if (value instanceof Integer i) return i;
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /** Parsea un valor double del Map de Last.fm. */
    private Double parseDouble(Object value) {
        if (value instanceof Double d) return d;
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS — HTTP
    // ─────────────────────────────────────────────────────────────────────────

    /** Construye la URL de la API de Last.fm con el método y parámetros indicados. */
    private String buildUrl(String method, Map<String, String> additionalParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(API_BASE_URL)
                .queryParam("method", method)
                .queryParam("api_key", apiKey)
                .queryParam("format", "json");

        if (additionalParams != null) {
            additionalParams.forEach(builder::queryParam);
        }

        return builder.toUriString();
    }

    /**
     * Ejecuta la petición HTTP y maneja errores HTTP y errores de negocio de Last.fm.
     * Last.fm devuelve errores con HTTP 200 pero con un campo {@code error} en el body.
     */
    private Map<String, Object> executeRequest(String url, String operationDescription) {
        try {
            log.debug("Realizando petición GET a: {}", url.replaceAll("api_key=[^&]+", "api_key=***"));

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // Last.fm devuelve errores en el body con HTTP 200
                if (body.containsKey("error")) {
                    handleLastFmError(body);
                }

                log.info("✓ {} obtenida exitosamente de {} API", operationDescription, API_NAME);
                return body;
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

    /** Maneja errores de negocio de Last.fm (vienen en el body con HTTP 200). */
    private void handleLastFmError(Map<String, Object> body) {
        Integer errorCode = (Integer) body.get("error");
        String errorMessage = (String) body.get("message");

        log.error("✗ Error de {} API - Código: {}, Mensaje: {}", API_NAME, errorCode, errorMessage);

        switch (errorCode) {
            case 6 -> {
                // Track/artist not found — no es un error grave
                log.warn("Track no encontrado en {}: {}", API_NAME, errorMessage);
                throw new ExternalApiException(API_NAME, errorCode, errorMessage);
            }
            case 10, 26 -> throw new ApiAuthenticationException(API_NAME, errorMessage);
            case 29 -> throw new ApiRateLimitException(API_NAME, errorMessage);
            default -> throw new ExternalApiException(API_NAME, errorCode, errorMessage);
        }
    }

    /** Maneja errores HTTP 4xx de Last.fm. */
    private void handleClientError(HttpClientErrorException e) {
        int statusCode = e.getStatusCode().value();
        log.error("✗ Error del cliente ({}) en {} API: {}", statusCode, API_NAME, e.getMessage());

        if (statusCode == HttpStatus.UNAUTHORIZED.value() || statusCode == HttpStatus.FORBIDDEN.value()) {
            throw new ApiAuthenticationException(API_NAME, statusCode, "API Key inválida o sin permisos");
        } else if (statusCode == HttpStatus.TOO_MANY_REQUESTS.value()) {
            throw new ApiRateLimitException(API_NAME, "Límite de peticiones excedido");
        } else {
            throw new ExternalApiException(API_NAME, statusCode, "Error del cliente: " + e.getMessage(), e);
        }
    }

    /** Maneja errores HTTP 5xx de Last.fm. */
    private void handleServerError(HttpServerErrorException e) {
        int statusCode = e.getStatusCode().value();
        log.error("✗ Error del servidor ({}) en {} API: {}", statusCode, API_NAME, e.getMessage());
        throw new ApiServerException(API_NAME, statusCode,
                "El servidor de la API está experimentando problemas", e);
    }
}