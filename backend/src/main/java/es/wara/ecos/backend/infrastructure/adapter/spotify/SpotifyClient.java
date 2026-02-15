package es.wara.ecos.backend.infrastructure.adapter.spotify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.*;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class SpotifyClient {

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken() {
        log.info("Solicitando access token a Spotify API...");

        String url = "https://accounts.spotify.com/api/token";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            String body = "grant_type=client_credentials";

            log.debug("Realizando petición POST a: {}", url);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String token = (String) response.getBody().get("access_token");
                log.info("✓ Access token obtenido exitosamente (longitud: {} caracteres)", token.length());
                log.debug("Token: {}...", token.substring(0, Math.min(20, token.length())));
                return token;
            } else {
                log.error("✗ Respuesta inesperada de Spotify API. Status: {}", response.getStatusCode());
                throw new RuntimeException("Error al obtener token: respuesta inválida");
            }

        } catch (RestClientException e) {
            log.error("✗ Error al comunicarse con Spotify API: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener access token de Spotify", e);
        } catch (Exception e) {
            log.error("✗ Error inesperado al obtener access token: {}", e.getMessage(), e);
            throw new RuntimeException("Error inesperado al obtener access token", e);
        }
    }
}