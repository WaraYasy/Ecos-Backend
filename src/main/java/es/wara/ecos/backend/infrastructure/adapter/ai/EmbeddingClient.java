package es.wara.ecos.backend.infrastructure.adapter.ai;

import es.wara.ecos.backend.config.AiServiceProperties;
import es.wara.ecos.backend.domain.port.EmbeddingProvider;
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
 * Cliente HTTP para el microservicio de IA (FastAPI Python).
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Implementar {@link EmbeddingProvider}: envía texto al micro para generar el embedding.</li>
 *   <li>Añadir el header {@code X-API-Key} en todas las peticiones.</li>
 *   <li>Degradar con gracia: si el micro no está disponible o responde con error,
 *       registra en log y no propaga la excepción — el flujo de guardado de canciones
 *       no debe bloquearse por indisponibilidad del micro.</li>
 * </ul>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Slf4j
@Component
public class EmbeddingClient implements EmbeddingProvider {

    private static final String EMBEDDINGS_SONG_PATH = "/embeddings/song";

    private final RestTemplate restTemplate;
    private final AiServiceProperties properties;

    public EmbeddingClient(RestTemplate restTemplate, AiServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Implementación del puerto {@link EmbeddingProvider}.
     * Llama al micro Python y descarta el resultado — fire-and-forget.
     */
    @Override
    public void requestEmbedding(UUID songId, String text) {
        generateEmbedding(songId, text);
    }

    /**
     * Solicita al micro Python que genere y persista el embedding de una canción.
     *
     * <p>El texto que se embede se construye en Java siguiendo el formato acordado:
     * {@code "título · artista1, artista2 · tag1 tag2 · género1 género2"}.</p>
     *
     * @param songId identificador UUID de la canción
     * @param text   texto a embedar (título, artistas, tags y géneros concatenados)
     * @return el array de floats (384 dimensiones) si la llamada fue exitosa,
     *         o {@link Optional#empty()} si el micro no está disponible o devuelve error
     */
    public Optional<float[]> generateEmbedding(UUID songId, String text) {
        if (!properties.isAvailable()) {
            log.debug("Micro IA no configurado — omitiendo embedding para {}", songId);
            return Optional.empty();
        }

        try {
            HttpHeaders headers = buildHeaders();
            Map<String, String> body = Map.of(
                    "songId", songId.toString(),
                    "text",   text
            );

            ResponseEntity<EmbeddingResponse> response = restTemplate.exchange(
                    properties.getUrl() + EMBEDDINGS_SONG_PATH,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    EmbeddingResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.ofNullable(response.getBody().toFloatArray());
            }

            log.warn("Micro IA devolvió {} para embedding de {}", response.getStatusCode(), songId);
            return Optional.empty();

        } catch (RestClientException e) {
            log.warn("Error llamando al micro IA para embedding de {}: {}", songId, e.getMessage());
            return Optional.empty();
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.set("X-API-Key", properties.getApiKey());
        }
        return headers;
    }

    /** DTO interno para deserializar la respuesta del micro. */
    record EmbeddingResponse(String songId, List<Float> embedding, String modelVersion) {
        float[] toFloatArray() {
            if (embedding == null || embedding.isEmpty()) return null;
            float[] arr = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) arr[i] = embedding.get(i);
            return arr;
        }
    }
}