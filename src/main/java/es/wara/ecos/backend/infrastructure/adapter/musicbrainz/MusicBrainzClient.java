package es.wara.ecos.backend.infrastructure.adapter.musicbrainz;

import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzRecordingDetailDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzRecordingDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzReleaseDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzReleaseGroupDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzSearchResponseDTO;
import es.wara.ecos.backend.infrastructure.exception.ApiRateLimitException;
import es.wara.ecos.backend.infrastructure.exception.ApiServerException;
import es.wara.ecos.backend.infrastructure.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Cliente para la API de MusicBrainz.
 * <p>
 * Permite buscar soundtracks de películas, obtener tracklists y resolver
 * la relación inversa grabación → películas.
 * No requiere autenticación; solo exige un header User-Agent identificativo.
 * </p>
 * <p>
 * <b>Rate limit:</b> MusicBrainz permite 1 petición/segundo sin autenticación.
 * Este cliente aplica un throttle automático de {@value #REQUEST_INTERVAL_MS} ms
 * entre peticiones para respetar ese límite.
 * </p>
 * <p>
 * <b>Flujo A — Película → Canciones:</b>
 * <ol>
 *   <li>{@link #searchSoundtrackByMovie(String)} → release-groups de tipo Soundtrack</li>
 *   <li>{@link #getReleasesByGroup(String)} → ediciones del OST</li>
 *   <li>{@link #getRelease(String)} → tracklist completo</li>
 * </ol>
 * Fallback si no hay OST oficial: {@link #searchRecordingsByTag(String)}.
 * </p>
 * <p>
 * <b>Flujo B — Canción → Películas:</b>
 * <ol>
 *   <li>{@link #searchRecordingByArtistAndTitle(String, String)} → MBID de la grabación</li>
 *   <li>{@link #getMoviesForRecording(String)} → release-groups de tipo Soundtrack que la contienen</li>
 * </ol>
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 * @see <a href="https://musicbrainz.org/doc/MusicBrainz_API">MusicBrainz API docs</a>
 */
@Slf4j
@Component
public class MusicBrainzClient {

    private static final String API_NAME   = "MusicBrainz";
    private static final String BASE_URL   = "https://musicbrainz.org/ws/2";
    private static final String USER_AGENT = "ECOS/1.0 (ecos-backend)";

    /** Intervalo mínimo entre peticiones para respetar el rate limit de 1 req/s. */
    private static final long REQUEST_INTERVAL_MS = 1100;

    private volatile long lastRequestTime = 0;

    private final RestTemplate restTemplate;

    public MusicBrainzClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLUJO A — Película → Canciones del OST
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca release-groups de tipo Soundtrack por título de película.
     *
     * @param movieTitle título de la película a buscar
     * @return lista de release-groups coincidentes, vacía si no hay resultados
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<MusicBrainzReleaseGroupDTO> searchSoundtrackByMovie(String movieTitle) {
        String query = "secondarytype:soundtrack AND releasegroup:\"" + movieTitle + "\"";
        String url = UriComponentsBuilder.fromUriString(BASE_URL + "/release-group")
                .queryParam("query", query)
                .queryParam("fmt", "json")
                .queryParam("limit", 5)
                .build().toUriString();

        log.debug("Buscando soundtrack en MusicBrainz: {}", movieTitle);
        MusicBrainzSearchResponseDTO response = get(url, MusicBrainzSearchResponseDTO.class);
        List<MusicBrainzReleaseGroupDTO> groups = response != null ? response.getReleaseGroups() : null;
        return groups != null ? groups : Collections.emptyList();
    }

    /**
     * Obtiene las ediciones (releases) de un release-group.
     *
     * @param releaseGroupMbid MBID del release-group
     * @return lista de releases del grupo
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<MusicBrainzReleaseDTO> getReleasesByGroup(String releaseGroupMbid) {
        String url = UriComponentsBuilder.fromUriString(BASE_URL + "/release")
                .queryParam("release-group", releaseGroupMbid)
                .queryParam("fmt", "json")
                .build().toUriString();

        log.debug("Obteniendo releases del grupo: {}", releaseGroupMbid);
        MusicBrainzSearchResponseDTO response = get(url, MusicBrainzSearchResponseDTO.class);
        List<MusicBrainzReleaseDTO> releases = response != null ? response.getReleases() : null;
        return releases != null ? releases : Collections.emptyList();
    }

    /**
     * Obtiene el tracklist completo de un release concreto.
     *
     * @param releaseMbid MBID del release
     * @return el release con todos sus medios y pistas, o null si no existe
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public MusicBrainzReleaseDTO getRelease(String releaseMbid) {
        String url = UriComponentsBuilder.fromUriString(BASE_URL + "/release/" + releaseMbid)
                .queryParam("inc", "recordings+artist-credits")
                .queryParam("fmt", "json")
                .build().toUriString();

        log.debug("Obteniendo tracklist del release: {}", releaseMbid);
        return get(url, MusicBrainzReleaseDTO.class);
    }

    /**
     * Fallback del Flujo A: busca grabaciones etiquetadas con el nombre de la película.
     * Útil cuando no hay release-group oficial de tipo Soundtrack.
     *
     * @param movieTitle nombre de la película (se usará como tag)
     * @return lista de grabaciones etiquetadas, vacía si no hay resultados
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<MusicBrainzRecordingDTO> searchRecordingsByTag(String movieTitle) {
        String query = "tag:\"" + movieTitle.toLowerCase() + "\"";
        String url = UriComponentsBuilder.fromUriString(BASE_URL + "/recording")
                .queryParam("query", query)
                .queryParam("fmt", "json")
                .queryParam("limit", 20)
                .build().toUriString();

        log.debug("Buscando grabaciones por tag '{}' en MusicBrainz", movieTitle);
        MusicBrainzSearchResponseDTO response = get(url, MusicBrainzSearchResponseDTO.class);
        var recordings = response != null ? response.getRecordings() : null;
        return recordings != null ? recordings : Collections.emptyList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLUJO B — Canción → Películas que la contienen
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca el MBID de la grabación que mejor coincide con el artista y título dados.
     * <p>
     * Primer paso del Flujo B. El MBID resultante se usa en
     * {@link #getMoviesForRecording(String)} para encontrar los soundtracks.
     * </p>
     *
     * @param artist nombre del artista
     * @param title  título de la canción
     * @return MBID de la primera grabación coincidente, o vacío si no se encontró
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public Optional<String> searchRecordingByArtistAndTitle(String artist, String title) {
        String query = "artist:\"" + artist + "\" AND recording:\"" + title + "\"";
        String url = UriComponentsBuilder.fromUriString(BASE_URL + "/recording")
                .queryParam("query", query)
                .queryParam("fmt", "json")
                .queryParam("limit", 1)
                .build().toUriString();

        log.debug("Buscando MBID de grabación para '{} - {}' en MusicBrainz", artist, title);
        MusicBrainzSearchResponseDTO response = get(url, MusicBrainzSearchResponseDTO.class);

        if (response == null || response.getRecordings() == null || response.getRecordings().isEmpty()) {
            log.debug("No se encontró grabación en MusicBrainz para '{} - {}'", artist, title);
            return Optional.empty();
        }

        String mbid = response.getRecordings().get(0).getId();
        log.debug("MBID encontrado para '{} - {}': {}", artist, title, mbid);
        return Optional.ofNullable(mbid);
    }

    /**
     * Obtiene los release-groups de tipo Soundtrack que contienen la grabación indicada.
     * <p>
     * Segundo paso del Flujo B. El título de cada release-group devuelto es el nombre
     * del soundtrack (película) donde aparece la canción.
     * </p>
     *
     * @param recordingMbid MBID de la grabación obtenido de {@link #searchRecordingByArtistAndTitle}
     * @return release-groups de tipo Soundtrack que incluyen esta grabación; vacía si ninguno
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<MusicBrainzReleaseGroupDTO> getMoviesForRecording(String recordingMbid) {
        String url = UriComponentsBuilder.fromUriString(BASE_URL + "/recording/" + recordingMbid)
                .queryParam("inc", "release-groups")
                .queryParam("fmt", "json")
                .build().toUriString();

        log.debug("Obteniendo release-groups para grabación: {}", recordingMbid);
        MusicBrainzRecordingDetailDTO detail = get(url, MusicBrainzRecordingDetailDTO.class);

        if (detail == null || detail.getReleaseGroups() == null) {
            return Collections.emptyList();
        }

        List<MusicBrainzReleaseGroupDTO> soundtracks = detail.getReleaseGroups().stream()
                .filter(rg -> rg.getSecondaryTypes() != null
                        && rg.getSecondaryTypes().stream()
                               .anyMatch(t -> "Soundtrack".equalsIgnoreCase(t)))
                .toList();

        log.debug("Encontrados {} soundtracks para grabación {}", soundtracks.size(), recordingMbid);
        return soundtracks;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos internos
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Aplica el throttle de 1 req/s requerido por MusicBrainz.
     * Bloquea el hilo el tiempo necesario para respetar el intervalo mínimo.
     */
    private synchronized void throttle() {
        long now = System.currentTimeMillis();
        long waitMs = REQUEST_INTERVAL_MS - (now - lastRequestTime);
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }

    /** Ejecuta una petición GET con User-Agent y throttle, lanzando excepciones tipadas. */
    private <T> T get(String url, Class<T> responseType) {
        throttle();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            return response.getBody();

        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            log.error("Error del cliente ({}) en MusicBrainz: {}", status, e.getMessage());
            if (status == 429) {
                throw new ApiRateLimitException(API_NAME, "Límite de peticiones excedido");
            }
            throw new ExternalApiException(API_NAME, status, e.getMessage(), e);

        } catch (HttpServerErrorException e) {
            int status = e.getStatusCode().value();
            log.error("Error del servidor ({}) en MusicBrainz: {}", status, e.getMessage());
            // 503 también puede indicar rate limit en MusicBrainz
            if (status == 503) {
                throw new ApiRateLimitException(API_NAME, "Servicio no disponible (posible rate limit)");
            }
            throw new ApiServerException(API_NAME, status, "El servidor de MusicBrainz no está disponible", e);

        } catch (ResourceAccessException e) {
            log.error("Error de conexión con MusicBrainz: {}", e.getMessage());
            throw new ExternalApiException(API_NAME, "Error de conexión: " + e.getMessage(), e);
        }
    }

}