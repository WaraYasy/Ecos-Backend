package es.wara.ecos.backend.infrastructure.adapter.ai;

import es.wara.ecos.backend.config.AiServiceProperties;
import es.wara.ecos.backend.domain.port.SemanticSearchProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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
 * GET /search/semantic?q=canciones+de+peliculas+tristes&amp;limit=20
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
        return doSearch("/search/semantic", query, limit);
    }

    @Override
    public List<UUID> searchEnhanced(String query, int limit) {
        return doSearch("/search/semantic/enhanced", query, limit);
    }

    private List<UUID> doSearch(String path, String query, int limit) {
        if (!properties.isAvailable() || query == null || query.isBlank()) return List.of();
        try {
            String url = UriComponentsBuilder
                    .fromUriString(properties.getUrl() + path)
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
                if (response.getBody().expandedQuery() != null) {
                    log.debug("  [semantic enhanced] query expandida: '{}'", response.getBody().expandedQuery());
                }
                return response.getBody().results().stream()
                        .map(r -> {
                            try { return UUID.fromString(r.songId()); }
                            catch (IllegalArgumentException e) { return null; }
                        })
                        .filter(id -> id != null)
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (HttpClientErrorException e) {
            String requestId = e.getResponseHeaders() != null
                    ? e.getResponseHeaders().getFirst("X-Request-ID") : null;
            if (e.getStatusCode().value() == 429) {
                String retryAfter = e.getResponseHeaders() != null
                        ? e.getResponseHeaders().getFirst("Retry-After") : null;
                log.warn("Búsqueda semántica ({}): 429 — Retry-After={}s [X-Request-ID={}]",
                        path, retryAfter, requestId);
            } else {
                log.debug("Búsqueda semántica ({}) error {} para '{}' [X-Request-ID={}]",
                        path, e.getStatusCode().value(), query, requestId);
            }
            return List.of();
        } catch (RestClientException e) {
            log.debug("Búsqueda semántica ({}) no disponible para '{}': {}", path, query, e.getMessage());
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
    record SemanticSearchResponse(String query, String expandedQuery, List<SemanticResult> results) {}
}