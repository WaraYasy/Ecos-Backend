package es.wara.ecos.backend.infrastructure.adapter.ai;

import es.wara.ecos.backend.config.AiServiceProperties;
import es.wara.ecos.backend.domain.port.SemanticSearchProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cliente HTTP para la búsqueda semántica del microservicio de IA.
 *
 * <pre>
 * GET /search/semantic?q=canciones+de+peliculas+tristes&limit=20
 * </pre>
 *
 * <p>Devuelve UUIDs de canciones ordenados por similitud coseno con la consulta.
 * Si el micro no está disponible, devuelve lista vacía sin propagar el error.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Component
public class SemanticSearchClient implements SemanticSearchProvider {

    private final RestTemplate restTemplate;
    private final AiServiceProperties properties;

    public SemanticSearchClient(RestTemplate restTemplate, AiServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public List<UUID> search(String query, int limit) {
        if (!properties.isAvailable() || query == null || query.isBlank()) return List.of();
        try {
            String url = UriComponentsBuilder
                    .fromUriString(properties.getUrl() + "/search/semantic")
                    .queryParam("q", query)
                    .queryParam("limit", limit)
                    .build().toUriString();

            ResponseEntity<SemanticSearchResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    SemanticSearchResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                    && response.getBody().results() != null) {
                return response.getBody().results().stream()
                        .map(r -> {
                            try { return UUID.fromString(r.songId()); }
                            catch (IllegalArgumentException e) { return null; }
                        })
                        .filter(id -> id != null)
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (RestClientException e) {
            log.debug("Búsqueda semántica no disponible para '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.set("X-API-Key", properties.getApiKey());
        }
        return headers;
    }

    record SemanticResult(String songId, Double score) {}
    record SemanticSearchResponse(String query, List<SemanticResult> results) {}
}