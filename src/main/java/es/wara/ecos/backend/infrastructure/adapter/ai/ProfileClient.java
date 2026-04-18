package es.wara.ecos.backend.infrastructure.adapter.ai;

import es.wara.ecos.backend.config.AiServiceProperties;
import es.wara.ecos.backend.domain.model.MusicProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cliente HTTP para el endpoint de perfil musical del microservicio de IA.
 *
 * <pre>GET /profile/{userId}</pre>
 *
 * Si el micro no responde devuelve {@code Optional.empty()} para que el caller
 * caiga al cálculo local de {@code CalculateUserProfileUseCase}.
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Component
public class ProfileClient {

    private final RestTemplate restTemplate;
    private final AiServiceProperties properties;

    public ProfileClient(RestTemplate restTemplate, AiServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties   = properties;
    }

    public Optional<MusicProfile> getProfile(UUID userId) {
        if (!properties.isAvailable()) return Optional.empty();
        try {
            ResponseEntity<ProfileResponse> response = restTemplate.exchange(
                    properties.getUrl() + "/profile/" + userId,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    ProfileResponse.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody().toMusicProfile(userId));
            }
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Micro de perfil no disponible para userId={}: {}", userId, e.getMessage());
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

    record ProfileResponse(
            Double avgEnergy,
            Double harmonicRichness,
            Double eclecticity,
            Map<String, Double> favoriteEras,
            List<String> topGenres
    ) {
        MusicProfile toMusicProfile(UUID userId) {
            return new MusicProfile(
                    userId,
                    avgEnergy        != null ? BigDecimal.valueOf(avgEnergy)        : BigDecimal.ZERO,
                    harmonicRichness != null ? BigDecimal.valueOf(harmonicRichness) : BigDecimal.ZERO,
                    eclecticity      != null ? BigDecimal.valueOf(eclecticity)      : BigDecimal.ZERO,
                    favoriteEras     != null ? favoriteEras : Map.of(),
                    topGenres        != null ? topGenres    : List.of()
            );
        }
    }
}