package es.wara.ecos.backend.infrastructure.adapter.itunes;

import es.wara.ecos.backend.infrastructure.adapter.itunes.dto.ItunesSearchResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Cliente para la iTunes Search API de Apple.
 *
 * <p>No requiere autenticación ni API key. Devuelve previews de 30 segundos
 * para la mayoría del catálogo musical. Se usa como fuente alternativa a Spotify,
 * que dejó de devolver {@code preview_url} en Development Mode (nov 2024).</p>
 *
 * <p>Endpoint: {@code GET https://itunes.apple.com/search}</p>
 *
 * @author Wara Pacheco
 * @see <a href="https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI">iTunes Search API</a>
 */
@Slf4j
@Component
public class ItunesClient {

    private static final String BASE_URL = "https://itunes.apple.com/search";

    private final RestTemplate restTemplate;

    public ItunesClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Busca un preview de 30s en iTunes para el título y artista dados.
     *
     * <p>Devuelve el {@code previewUrl} del primer resultado que sea una canción
     * ({@code kind=song}) y tenga preview disponible.</p>
     *
     * @param title  título de la canción
     * @param artist nombre del artista principal
     * @return URL del preview de 30s, o {@link Optional#empty()} si no se encontró
     */
    @Cacheable(cacheNames = "itunes-preview", key = "#title + '-' + #artist")
    @Retryable(retryFor = RestClientException.class, maxAttempts = 2,
               backoff = @Backoff(delay = 500))
    public Optional<String> findPreviewUrl(String title, String artist) {
        try {
            String term = (title + " " + artist).trim();
            String url = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("term", term)
                    .queryParam("media", "music")
                    .queryParam("entity", "song")
                    .queryParam("limit", 3)
                    .build()
                    .toUriString();

            ItunesSearchResponseDTO response = restTemplate.getForObject(url, ItunesSearchResponseDTO.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return Optional.empty();
            }

            // Preferir el primer resultado que sea song y tenga preview
            return response.getResults().stream()
                    .filter(t -> "song".equals(t.getKind()) && t.getPreviewUrl() != null)
                    .map(ItunesSearchResponseDTO.ItunesTrackDTO::getPreviewUrl)
                    .findFirst();

        } catch (Exception e) {
            log.debug("iTunes: no se encontró preview para '{} - {}': {}", title, artist, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Busca previews para una lista de canciones en batch.
     * Devuelve solo los que tienen preview disponible.
     *
     * @param queries lista de pares (title, artist) como {@code "title|artist"}
     * @return lista de preview URLs encontradas
     */
    public List<String> findPreviewUrls(List<String> queries) {
        return queries.stream()
                .map(q -> {
                    String[] parts = q.split("\\|", 2);
                    return parts.length == 2
                            ? findPreviewUrl(parts[0], parts[1])
                            : Optional.<String>empty();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}