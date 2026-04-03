package es.wara.ecos.backend.infrastructure.adapter.itunes;

import es.wara.ecos.backend.infrastructure.adapter.itunes.dto.ItunesSearchResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * Cliente HTTP para la iTunes Search API (sin autenticación).
 *
 * <p>Se usa exclusivamente para obtener URLs de preview de 30 segundos.
 * El CDN de Apple ({@code audio-ssl.itunes.apple.com}) funciona nativamente
 * con AVFoundation sin necesidad de User-Agent especial, a diferencia de Deezer.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
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
     * Busca un track en iTunes y devuelve su preview URL de 30 segundos.
     *
     * @param title  título de la canción
     * @param artist nombre del artista
     * @return URL de preview MP3, o vacío si no se encontró o no tiene preview
     */
    public Optional<String> findPreviewUrl(String title, String artist) {
        try {
            String term = (artist != null && !artist.isBlank())
                    ? artist + " " + title
                    : title;

            String url = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("term", term)
                    .queryParam("media", "music")
                    .queryParam("entity", "song")
                    .queryParam("limit", 1)
                    .build().toUriString();

            ItunesSearchResponseDTO response = restTemplate.getForObject(url, ItunesSearchResponseDTO.class);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                log.debug("iTunes: sin resultados para '{} - {}'", artist, title);
                return Optional.empty();
            }

            String previewUrl = response.getResults().get(0).getPreviewUrl();
            if (previewUrl == null || previewUrl.isBlank()) {
                log.debug("iTunes: track sin preview para '{} - {}'", artist, title);
                return Optional.empty();
            }

            return Optional.of(previewUrl);

        } catch (Exception e) {
            log.debug("iTunes: error buscando '{} - {}': {}", artist, title, e.getMessage());
            return Optional.empty();
        }
    }
}