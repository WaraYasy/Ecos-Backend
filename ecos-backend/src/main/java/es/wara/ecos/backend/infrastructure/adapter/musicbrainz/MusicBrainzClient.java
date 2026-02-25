package es.wara.ecos.backend.infrastructure.adapter.musicbrainz;

import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzReleaseDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzReleaseGroupDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzSearchResponseDTO;
import es.wara.ecos.backend.infrastructure.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Cliente para la API de MusicBrainz.
 * <p>
 * Permite buscar soundtracks de películas y obtener sus tracklists.
 * No requiere autenticación; solo exige un header User-Agent identificativo.
 * </p>
 * <p>
 * <b>Flujo para obtener el OST de una película:</b>
 * <ol>
 *   <li>{@link #searchSoundtrackByMovie(String)} → obtener release-groups de tipo Soundtrack</li>
 *   <li>{@link #getReleasesByGroup(String)} → obtener las ediciones del OST</li>
 *   <li>{@link #getRelease(String)} → obtener el tracklist completo</li>
 * </ol>
 * Si no hay release-group oficial, usar {@link #searchRecordingsByTag(String)} como fallback.
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

    private static final String API_NAME  = "MusicBrainz";
    private static final String BASE_URL  = "https://musicbrainz.org/ws/2";
    /** User-Agent requerido por MusicBrainz para identificar la aplicación. */
    private static final String USER_AGENT = "ECOS/1.0 (ecos-backend)";

    private final RestTemplate restTemplate;

    public MusicBrainzClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Busca release-groups de tipo Soundtrack por título de película.
     *
     * @param movieTitle título de la película a buscar
     * @return lista de release-groups coincidentes, vacía si no hay resultados
     */
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
     * Usar para elegir la edición oficial antes de pedir el tracklist.
     *
     * @param releaseGroupMbid MBID del release-group obtenido de {@link #searchSoundtrackByMovie}
     * @return lista de releases del grupo
     */
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
     * @param releaseMbid MBID del release obtenido de {@link #getReleasesByGroup}
     * @return el release con todos sus medios y pistas, o null si no existe
     */
    public MusicBrainzReleaseDTO getRelease(String releaseMbid) {
        String url = UriComponentsBuilder.fromUriString(BASE_URL + "/release/" + releaseMbid)
                .queryParam("inc", "recordings+artist-credits")
                .queryParam("fmt", "json")
                .build().toUriString();

        log.debug("Obteniendo tracklist del release: {}", releaseMbid);
        return get(url, MusicBrainzReleaseDTO.class);
    }

    /**
     * Fallback: busca grabaciones etiquetadas con el nombre de la película.
     * Útil cuando no hay release-group oficial de tipo Soundtrack.
     *
     * @param movieTitle nombre de la película (se usará como tag)
     * @return lista de grabaciones etiquetadas con el nombre de la película
     */
    public List<es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzRecordingDTO> searchRecordingsByTag(String movieTitle) {
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

    // -------------------------------------------------------------------------
    // Método interno de petición HTTP
    // -------------------------------------------------------------------------

    private <T> T get(String url, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Error del cliente ({}) en MusicBrainz: {}", e.getStatusCode().value(), e.getMessage());
            throw new ExternalApiException(API_NAME, e.getStatusCode().value(), e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            log.error("Error del servidor ({}) en MusicBrainz: {}", e.getStatusCode().value(), e.getMessage());
            throw new ExternalApiException(API_NAME, e.getStatusCode().value(), "El servidor de MusicBrainz no está disponible", e);
        } catch (ResourceAccessException e) {
            log.error("Error de conexión con MusicBrainz: {}", e.getMessage());
            throw new ExternalApiException(API_NAME, "Error de conexión: " + e.getMessage(), e);
        }
    }

}
