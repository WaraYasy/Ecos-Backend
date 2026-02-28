package es.wara.ecos.backend.infrastructure.adapter.spotify;

import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifyAudioFeaturesDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifySearchResponseDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.dto.SpotifyTrackDTO;
import es.wara.ecos.backend.infrastructure.exception.ApiRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests unitarios de SpotifyClient usando MockRestServiceServer.
 * <p>
 * Se instancia el cliente manualmente (sin Spring) y se interceptan las
 * llamadas HTTP a nivel de RestTemplate, sin necesidad de WireMock.
 * Los métodos marcados con @Retryable no actúan sin AOP de Spring,
 * por lo que se verifica solo el primer intento.
 * </p>
 */
class SpotifyClientTest {

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String API_BASE = "https://api.spotify.com/v1";
    private static final String TOKEN_RESPONSE =
            """
            {"access_token":"test-token","token_type":"Bearer","expires_in":3600}
            """;

    RestTemplate restTemplate;
    MockRestServiceServer mockServer;
    SpotifyClient spotifyClient;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        spotifyClient = new SpotifyClient(restTemplate);
        ReflectionTestUtils.setField(spotifyClient, "clientId", "test-id");
        ReflectionTestUtils.setField(spotifyClient, "clientSecret", "test-secret");
    }

    @Test
    void getAccessToken_success_cachesToken() {
        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        String token1 = spotifyClient.getAccessToken();
        String token2 = spotifyClient.getAccessToken(); // segunda llamada — desde caché

        assertThat(token1).isEqualTo("test-token");
        assertThat(token2).isEqualTo("test-token");
        mockServer.verify(); // solo 1 petición HTTP real
    }

    @Test
    void searchTracks_validQuery_returnsResponse() {
        String searchResponse = """
                {"tracks":{"items":[{"id":"t1","name":"Bohemian Rhapsody","duration_ms":355000}],
                 "total":1,"limit":5,"offset":0}}
                """;

        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(searchResponse, MediaType.APPLICATION_JSON));

        SpotifySearchResponseDTO result = spotifyClient.searchTracks("bohemian rhapsody", 5);

        assertThat(result).isNotNull();
        assertThat(result.getTracks()).isNotNull();
        assertThat(result.getTracks().getItems()).hasSize(1);
        assertThat(result.getTracks().getItems().get(0).getName()).isEqualTo("Bohemian Rhapsody");
    }

    @Test
    void searchTracks_rateLimitResponse_throwsApiRateLimitException() {
        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> spotifyClient.searchTracks("test query", 10))
                .isInstanceOf(ApiRateLimitException.class);
    }

    @Test
    void getTrackAudioFeatures_validId_returnsMappedDto() {
        String audioFeaturesResponse = """
                {"id":"track-123","energy":0.8,"tempo":120.0,"valence":0.7,
                 "danceability":0.6,"acousticness":0.1,"instrumentalness":0.0,
                 "speechiness":0.05,"loudness":-5.0,"key":5,"mode":1,"time_signature":4}
                """;

        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(API_BASE + "/audio-features/track-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(audioFeaturesResponse, MediaType.APPLICATION_JSON));

        SpotifyAudioFeaturesDTO result = spotifyClient.getTrackAudioFeatures("track-123");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("track-123");
        assertThat(result.getEnergy()).isEqualTo(0.8);
        assertThat(result.getKey()).isEqualTo(5);
    }

    @Test
    void getTrack_401_renewsTokenAndRetries() {
        String trackResponse = """
                {"id":"track-abc","name":"Test Song","duration_ms":200000,
                 "album":{"id":"a1","name":"Test Album","release_date":"2020-01-01"},
                 "artists":[{"id":"ar1","name":"Test Artist"}]}
                """;

        // 1. Token inicial
        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        // 2. Primer intento → 401
        mockServer.expect(requestTo(API_BASE + "/tracks/track-abc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        // 3. Renovación de token tras invalidación
        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        // 4. Reintento → 200
        mockServer.expect(requestTo(API_BASE + "/tracks/track-abc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(trackResponse, MediaType.APPLICATION_JSON));

        SpotifyTrackDTO result = spotifyClient.getTrack("track-abc");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("track-abc");
        assertThat(result.getName()).isEqualTo("Test Song");
        mockServer.verify();
    }
}
