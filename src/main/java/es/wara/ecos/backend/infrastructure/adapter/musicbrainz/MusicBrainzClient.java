package es.wara.ecos.backend.infrastructure.adapter.musicbrainz;

import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzArtistDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzRecordingDetailDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzRecordingDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzReleaseDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzReleaseGroupDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzSearchResponseDTO;
import es.wara.ecos.backend.infrastructure.exception.ApiRateLimitException;
import es.wara.ecos.backend.infrastructure.exception.ApiServerException;
import es.wara.ecos.backend.infrastructure.exception.ExternalApiException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
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

import java.time.Duration;
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
 * Este cliente aplica un throttle automático
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

    /** 1 petición/segundo — límite oficial de MusicBrainz sin autenticación. */
    private final Bucket rateLimiter = Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(1).refillGreedy(1, Duration.ofSeconds(1)).build())
            .build();

    private final RestTemplate restTemplate;

    public MusicBrainzClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLUJO C — País → Artistas de ese origen
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca artistas cuya área de origen coincide con el país dado.
     * <p>
     * Usa la búsqueda Lucene de MusicBrainz: {@code area:"Spain" AND type:person}.
     * Devuelve artistas con su nombre y área para permitir enriquecer el campo
     * {@code country} de la entidad {@code artists} en la base de datos local.
     * </p>
     *
     * @param country nombre del país en inglés (ej. "Spain", "Japan", "United Kingdom")
     * @param limit   máximo de artistas a devolver
     * @return lista de DTOs de artista con área; vacía si no hay resultados
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<MusicBrainzArtistDTO> searchArtistsByCountry(String country, int limit) {
        String query = "area:\"" + country + "\" AND type:person";
        String url = UriComponentsBuilder.fromUriString(BASE_URL + "/artist")
                .queryParam("query", query)
                .queryParam("fmt", "json")
                .queryParam("limit", limit)
                .build().toUriString();

        log.debug("Buscando artistas de '{}' en MusicBrainz", country);
        MusicBrainzSearchResponseDTO response = get(url, MusicBrainzSearchResponseDTO.class);
        var artists = response != null ? response.getArtists() : null;
        return artists != null ? artists : Collections.emptyList();
    }

    /**
     * Busca el país de origen de un artista por su nombre.
     * Usa la búsqueda Lucene: {@code artist:"nombre" AND type:person}.
     *
     * @param artistName nombre del artista
     * @return nombre del país en inglés (ej. "United States"), o vacío si no se encuentra
     */
    @Retryable(
        retryFor = {ApiRateLimitException.class, ApiServerException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public Optional<String> searchArtistCountryByName(String artistName) {
        String query = "artist:\"" + artistName + "\" AND type:person";
        String url = UriComponentsBuilder.fromUriString(BASE_URL + "/artist")
                .queryParam("query", query)
                .queryParam("fmt", "json")
                .queryParam("limit", 1)
                .build().toUriString();

        log.debug("Buscando país de '{}' en MusicBrainz", artistName);
        MusicBrainzSearchResponseDTO response = get(url, MusicBrainzSearchResponseDTO.class);
        if (response == null || response.getArtists() == null || response.getArtists().isEmpty()) {
            return Optional.empty();
        }
        MusicBrainzArtistDTO artist = response.getArtists().get(0);
        if (artist.getArea() == null || artist.getArea().getName() == null) {
            return Optional.empty();
        }
        return Optional.of(artist.getArea().getName());
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

    /** Ejecuta una petición GET con User-Agent y throttle, lanzando excepciones tipadas. */
    private <T> T get(String url, Class<T> responseType) {
        try {
            rateLimiter.asBlocking().consume(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException(API_NAME, "Throttle interrumpido");
        }
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