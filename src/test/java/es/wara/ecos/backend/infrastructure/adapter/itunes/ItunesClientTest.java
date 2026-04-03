package es.wara.ecos.backend.infrastructure.adapter.itunes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ItunesClientTest {

    private static final String ITUNES_SEARCH = "https://itunes.apple.com/search";

    private static final String TRACK_WITH_PREVIEW = """
            {
              "resultCount": 1,
              "results": [{
                "trackId": 580707864,
                "trackName": "Lose Yourself",
                "artistName": "Eminem",
                "previewUrl": "https://audio-ssl.itunes.apple.com/lose-yourself-preview.m4a"
              }]
            }
            """;

    private static final String TRACK_WITHOUT_PREVIEW = """
            {
              "resultCount": 1,
              "results": [{
                "trackId": 123,
                "trackName": "Some Song",
                "artistName": "Some Artist"
              }]
            }
            """;

    private static final String EMPTY_RESPONSE = """
            {"resultCount": 0, "results": []}
            """;

    RestTemplate restTemplate;
    MockRestServiceServer mockServer;
    ItunesClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = new ItunesClient(restTemplate);
    }

    @Test
    void findPreviewUrl_found_returnsUrl() {
        mockServer.expect(requestTo(containsString(ITUNES_SEARCH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TRACK_WITH_PREVIEW, MediaType.APPLICATION_JSON));

        Optional<String> result = client.findPreviewUrl("Lose Yourself", "Eminem");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("https://audio-ssl.itunes.apple.com/lose-yourself-preview.m4a");
    }

    @Test
    void findPreviewUrl_noResults_returnsEmpty() {
        mockServer.expect(requestTo(containsString(ITUNES_SEARCH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(EMPTY_RESPONSE, MediaType.APPLICATION_JSON));

        Optional<String> result = client.findPreviewUrl("nonexistent", "nobody");

        assertThat(result).isEmpty();
    }

    @Test
    void findPreviewUrl_trackWithoutPreview_returnsEmpty() {
        mockServer.expect(requestTo(containsString(ITUNES_SEARCH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TRACK_WITHOUT_PREVIEW, MediaType.APPLICATION_JSON));

        Optional<String> result = client.findPreviewUrl("Some Song", "Some Artist");

        assertThat(result).isEmpty();
    }

    @Test
    void findPreviewUrl_serverError_returnsEmpty() {
        mockServer.expect(requestTo(containsString(ITUNES_SEARCH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        Optional<String> result = client.findPreviewUrl("Any Song", "Any Artist");

        assertThat(result).isEmpty();
    }

    @Test
    void findPreviewUrl_nullArtist_searchesByTitleOnly() {
        mockServer.expect(requestTo(containsString("term=Lose")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TRACK_WITH_PREVIEW, MediaType.APPLICATION_JSON));

        Optional<String> result = client.findPreviewUrl("Lose Yourself", null);

        assertThat(result).isPresent();
    }
}