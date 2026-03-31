package es.wara.ecos.backend.infrastructure.adapter.itunes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ItunesClientTest {

    private static final String ITUNES_BASE = "https://itunes.apple.com/search";

    RestTemplate restTemplate;
    MockRestServiceServer mockServer;
    ItunesClient itunesClient;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        itunesClient = new ItunesClient(restTemplate, new ObjectMapper());
    }

    @Test
    void findPreviewUrl_trackFound_returnsPreviewUrl() {
        mockServer.expect(requestTo(containsString(ITUNES_BASE)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "resultCount": 1,
                          "results": [{
                            "wrapperType": "track",
                            "kind": "song",
                            "trackName": "Bohemian Rhapsody",
                            "artistName": "Queen",
                            "previewUrl": "https://audio-ssl.itunes.apple.com/preview.m4a"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<String> result = itunesClient.findPreviewUrl("Bohemian Rhapsody", "Queen");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("https://audio-ssl.itunes.apple.com/preview.m4a");
    }

    @Test
    void findPreviewUrl_noResults_returnsEmpty() {
        mockServer.expect(requestTo(containsString(ITUNES_BASE)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"resultCount": 0, "results": []}
                        """, MediaType.APPLICATION_JSON));

        Optional<String> result = itunesClient.findPreviewUrl("Unknown Song", "Unknown Artist");

        assertThat(result).isEmpty();
    }

    @Test
    void findPreviewUrl_resultWithoutPreview_returnsEmpty() {
        mockServer.expect(requestTo(containsString(ITUNES_BASE)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "resultCount": 1,
                          "results": [{
                            "wrapperType": "track",
                            "kind": "song",
                            "trackName": "Some Song",
                            "artistName": "Some Artist",
                            "previewUrl": null
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<String> result = itunesClient.findPreviewUrl("Some Song", "Some Artist");

        assertThat(result).isEmpty();
    }

    @Test
    void findPreviewUrl_skipsNonSongResults_returnsFirstSongPreview() {
        mockServer.expect(requestTo(containsString(ITUNES_BASE)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "resultCount": 2,
                          "results": [
                            {
                              "kind": "music-video",
                              "trackName": "Video Track",
                              "artistName": "Artist",
                              "previewUrl": "https://video-preview.m4v"
                            },
                            {
                              "kind": "song",
                              "trackName": "Audio Track",
                              "artistName": "Artist",
                              "previewUrl": "https://audio-preview.m4a"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<String> result = itunesClient.findPreviewUrl("Audio Track", "Artist");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("https://audio-preview.m4a");
    }

    @Test
    void findPreviewUrl_serverError_returnsEmpty() {
        mockServer.expect(requestTo(containsString(ITUNES_BASE)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        Optional<String> result = itunesClient.findPreviewUrl("Any Song", "Any Artist");

        assertThat(result).isEmpty();
    }

    @Test
    void findPreviewUrl_includesTitleAndArtistInQuery() {
        // UriComponentsBuilder usa percent-encoding (%20), no form-encoding (+)
        mockServer.expect(requestTo(allOf(containsString("Dreams"), containsString("Fleetwood"))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"resultCount": 0, "results": []}
                        """, MediaType.APPLICATION_JSON));

        itunesClient.findPreviewUrl("Dreams", "Fleetwood Mac");

        mockServer.verify();
    }
}