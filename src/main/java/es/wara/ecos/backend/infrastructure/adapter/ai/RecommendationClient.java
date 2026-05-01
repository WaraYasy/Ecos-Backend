package es.wara.ecos.backend.infrastructure.adapter.ai;

import es.wara.ecos.backend.config.AiServiceProperties;
import es.wara.ecos.backend.domain.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cliente HTTP para los endpoints de recomendaciones del microservicio de IA.
 *
 * <ul>
 *   <li>{@code GET /recommendations/{userId}} — recomendaciones personalizadas.</li>
 *   <li>{@code GET /recommendations/crossover?userId1=&userId2=} — recomendaciones cruzadas.</li>
 * </ul>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Component
public class RecommendationClient {

    private final RestTemplate restTemplate;
    private final AiServiceProperties properties;

    public RecommendationClient(RestTemplate restTemplate, AiServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Obtiene recomendaciones personalizadas para un usuario.
     *
     * @param userId UUID del usuario
     * @return lista de pares (songId, score) ordenados por relevancia, o vacío si el micro no responde
     * @throws IllegalStateException si el usuario no tiene historial (micro devuelve 422)
     */
    public List<ScoredSongId> getRecommendations(UUID userId) {
        if (!properties.isAvailable()) return List.of();
        try {
            ResponseEntity<RecommendationsResponse> response = restTemplate.exchange(
                    properties.getUrl() + "/recommendations/" + userId,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    RecommendationsResponse.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().recommendations() != null
                        ? response.getBody().recommendations()
                        : List.of();
            }
            return List.of();
        } catch (org.springframework.web.client.HttpClientErrorException.UnprocessableEntity e) {
            throw new ConflictException("El usuario no tiene historial de interacciones suficiente");
        } catch (RestClientException e) {
            log.warn("Error obteniendo recomendaciones para {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Obtiene recomendaciones cruzadas para dos usuarios.
     *
     * @param userAId UUID del primer usuario (el autenticado)
     * @param userBId UUID del segundo usuario
     * @return resultado crossover con songs y overlapCount, o vacío si el micro no responde
     */
    public Optional<CrossoverResult> getCrossoverRecommendations(UUID userAId, UUID userBId) {
        if (!properties.isAvailable()) return Optional.empty();
        try {
            String url = properties.getUrl() + "/recommendations/crossover"
                    + "?userId1=" + userAId + "&userId2=" + userBId;
            ResponseEntity<CrossoverResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    CrossoverResponse.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                CrossoverResponse body = response.getBody();
                List<ScoredSongId> recs = body.results() != null ? body.results() : List.of();
                int overlap = body.overlapCount() != null ? body.overlapCount() : 0;
                return Optional.of(new CrossoverResult(recs, overlap));
            }
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Crossover ({} + {}): {}", userAId, userBId, e.getMessage());
            return Optional.empty();
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

    // ── DTOs internos ───────────────────────────────────────────────────────

    public record ScoredSongId(String songId, Double score) {}

    record RecommendationsResponse(String userId, List<ScoredSongId> recommendations) {}

    record CrossoverResponse(
            String userId1,
            String userId2,
            Integer overlapCount,
            List<ScoredSongId> results
    ) {}

    public record CrossoverResult(List<ScoredSongId> recommendations, int overlapCount) {}
}
