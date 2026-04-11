package es.wara.ecos.backend.infrastructure.adapter.ai;

import es.wara.ecos.backend.config.AiServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SemanticSearchClientTest {

    private static final String BASE_URL = "http://ai-micro.test";
    private static final UUID ID_1 = UUID.fromString("11111111-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("22222222-0000-0000-0000-000000000002");

    RestTemplate restTemplate;
    MockRestServiceServer mockServer;
    SemanticSearchClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        AiServiceProperties props = new AiServiceProperties();
        props.setUrl(BASE_URL);
        props.setApiKey("test-key");

        client = new SemanticSearchClient(restTemplate, props);
    }

    @Test
    void search_success_returnsUuidsInOrder() {
        String json = """
                {"query":"canciones tristes","results":[
                  {"songId":"%s","score":0.91},
                  {"songId":"%s","score":0.78}
                ]}
                """.formatted(ID_1, ID_2);

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/search/semantic")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", "test-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<UUID> result = client.search("canciones tristes", 10);

        assertThat(result).containsExactly(ID_1, ID_2);
        mockServer.verify();
    }

    @Test
    void search_serverError_returnsEmptyList() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/search/semantic")))
                .andRespond(withServerError());

        List<UUID> result = client.search("query", 10);

        assertThat(result).isEmpty();
    }

    @Test
    void search_emptyQuery_returnsEmptyListWithoutCallingServer() {
        List<UUID> result = client.search("  ", 10);

        assertThat(result).isEmpty();
        mockServer.verify(); // ninguna llamada HTTP
    }

    @Test
    void search_microNotConfigured_returnsEmptyListWithoutCallingServer() {
        AiServiceProperties emptyProps = new AiServiceProperties();
        SemanticSearchClient unconfigured = new SemanticSearchClient(restTemplate, emptyProps);

        List<UUID> result = unconfigured.search("query", 10);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void searchEnhanced_success_returnsUuidsAndLogsExpandedQuery() {
        String json = """
                {"query":"música para una boda","expandedQuery":"romantic soft acoustic gentle happy uplifting","results":[
                  {"songId":"%s","score":0.91},
                  {"songId":"%s","score":0.85}
                ]}
                """.formatted(ID_1, ID_2);

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/search/semantic/enhanced")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-Key", "test-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<UUID> result = client.searchEnhanced("música para una boda", 10);

        assertThat(result).containsExactly(ID_1, ID_2);
        mockServer.verify();
    }

    @Test
    void searchEnhanced_groqUnavailable_returnsEmptyList() {
        // El micro devuelve 503 cuando GROQ_API_KEY no está configurada
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/search/semantic/enhanced")))
                .andRespond(withServiceUnavailable());

        List<UUID> result = client.searchEnhanced("canciones tristes", 10);

        assertThat(result).isEmpty();
    }

    @Test
    void search_malformedUuidInResponse_skipsIt() {
        String json = """
                {"query":"test","results":[
                  {"songId":"not-a-uuid","score":0.9},
                  {"songId":"%s","score":0.7}
                ]}
                """.formatted(ID_1);

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/search/semantic")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<UUID> result = client.search("test", 10);

        assertThat(result).containsExactly(ID_1);
    }
}