package es.wara.ecos.backend.infrastructure.adapter.ai;

import es.wara.ecos.backend.config.AiServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EmbeddingClientTest {

    private static final String BASE_URL = "http://ai-micro.test";
    private static final UUID SONG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String TEXT = "Chariots of Fire · Vangelis · soundtrack epic";

    RestTemplate restTemplate;
    MockRestServiceServer mockServer;
    EmbeddingClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        AiServiceProperties props = new AiServiceProperties();
        props.setUrl(BASE_URL);
        props.setApiKey("test-key");

        client = new EmbeddingClient(restTemplate, props);
    }

    @Test
    void generateEmbedding_success_returnsFloatArray() {
        String json = """
                {"songId":"%s","embedding":[0.1,0.2,0.3],"modelVersion":"paraphrase-multilingual-MiniLM-L12-v2"}
                """.formatted(SONG_ID);

        mockServer.expect(requestTo(BASE_URL + "/embeddings/song"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "test-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<float[]> result = client.generateEmbedding(SONG_ID, TEXT);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(3);
        assertThat(result.get()[0]).isEqualTo(0.1f, org.assertj.core.api.Assertions.within(0.001f));
        mockServer.verify();
    }

    @Test
    void generateEmbedding_serverError_returnsEmpty() {
        mockServer.expect(requestTo(BASE_URL + "/embeddings/song"))
                .andRespond(withServerError());

        Optional<float[]> result = client.generateEmbedding(SONG_ID, TEXT);

        assertThat(result).isEmpty();
    }

    @Test
    void generateEmbedding_microNotConfigured_returnsEmptyWithoutCallingServer() {
        AiServiceProperties emptyProps = new AiServiceProperties();
        // url vacía → isAvailable() = false
        EmbeddingClient unconfiguredClient = new EmbeddingClient(restTemplate, emptyProps);

        Optional<float[]> result = unconfiguredClient.generateEmbedding(SONG_ID, TEXT);

        assertThat(result).isEmpty();
        mockServer.verify(); // ninguna llamada HTTP fue hecha
    }

    @Test
    void requestEmbedding_delegatesToGenerateEmbedding_doesNotThrow() {
        String json = """
                {"songId":"%s","embedding":[0.5],"modelVersion":"test"}
                """.formatted(SONG_ID);
        mockServer.expect(requestTo(BASE_URL + "/embeddings/song"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        // No debe lanzar excepción aunque el embedding sea de 1 dimensión
        client.requestEmbedding(SONG_ID, TEXT);

        mockServer.verify();
    }
}