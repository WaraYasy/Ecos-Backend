package es.wara.ecos.backend.infrastructure.adapter.deezer;

import es.wara.ecos.backend.domain.model.Song;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

/**
 * Tests unitarios de DeezerClient usando MockRestServiceServer (sin Spring, sin WireMock).
 */
class DeezerClientTest {

    private static final String DEEZER_SEARCH = "https://api.deezer.com/search";

    private static final String TRACK_RESPONSE = """
            {
              "data": [{
                "id": 3135556,
                "title": "Lose Yourself",
                "duration": 326,
                "preview": "https://cdns-preview.dzcdn.net/stream/lose-yourself.mp3",
                "isrc": "USUM70209454",
                "artist": {
                  "id": 13,
                  "name": "Eminem",
                  "picture_medium": "https://api.deezer.com/artist/13/image?size=medium"
                },
                "album": {
                  "id": 302127,
                  "title": "8 Mile",
                  "cover_medium": "https://api.deezer.com/album/302127/image?size=medium"
                }
              }],
              "total": 1
            }
            """;

    private static final String EMPTY_RESPONSE = """
            {"data": [], "total": 0}
            """;

    RestTemplate restTemplate;
    MockRestServiceServer mockServer;
    DeezerClient deezerClient;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        deezerClient = new DeezerClient(restTemplate);
    }

    @Test
    void searchTrack_found_mapsAllFields() {
        mockServer.expect(requestTo(containsString(DEEZER_SEARCH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TRACK_RESPONSE, MediaType.APPLICATION_JSON));

        Optional<Song> result = deezerClient.searchTrack("Lose Yourself", "Eminem");

        assertThat(result).isPresent();
        Song song = result.get();
        assertThat(song.getTitle()).isEqualTo("Lose Yourself");
        assertThat(song.getDurationMs()).isEqualTo(326_000);
        assertThat(song.getPreviewUrl()).isNull(); // Deezer no aporta preview — lo gestiona ItunesClient
        assertThat(song.getIsrc()).isEqualTo("USUM70209454");
        assertThat(song.getArtists()).hasSize(1);
        assertThat(song.getArtists().get(0).getName()).isEqualTo("Eminem");
        assertThat(song.getAlbum()).isNotNull();
        assertThat(song.getAlbum().getTitle()).isEqualTo("8 Mile");
        assertThat(song.getAlbum().getCoverUrl()).isEqualTo("https://api.deezer.com/album/302127/image?size=medium");
    }

    @Test
    void searchTrack_noResults_returnsEmpty() {
        mockServer.expect(requestTo(containsString(DEEZER_SEARCH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(EMPTY_RESPONSE, MediaType.APPLICATION_JSON));

        Optional<Song> result = deezerClient.searchTrack("nonexistent song xyz", "nobody");

        assertThat(result).isEmpty();
    }

    @Test
    void searchTrack_serverError_returnsEmpty() {
        mockServer.expect(requestTo(containsString(DEEZER_SEARCH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        Optional<Song> result = deezerClient.searchTrack("Any Song", "Any Artist");

        assertThat(result).isEmpty();
    }

    @Test
    void searchTrack_emptyArtist_stillSearches() {
        mockServer.expect(requestTo(containsString(DEEZER_SEARCH)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TRACK_RESPONSE, MediaType.APPLICATION_JSON));

        Optional<Song> result = deezerClient.searchTrack("Lose Yourself", "");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Lose Yourself");
    }
}
