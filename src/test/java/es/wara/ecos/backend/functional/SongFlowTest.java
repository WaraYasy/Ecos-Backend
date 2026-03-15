package es.wara.ecos.backend.functional;

import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.support.BaseFunctionalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests funcionales para los endpoints de canciones.
 *
 * MusicProvider está mockeado para evitar llamadas reales a APIs externas
 * en los tests de canciones relacionadas, creando un contexto Spring propio.
 */
class SongFlowTest extends BaseFunctionalTest {

    @MockitoBean
    MusicProvider musicProvider;

    @Autowired
    JdbcTemplate jdbc;

    private UUID songId;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE TABLE artists, users CASCADE");
        insertSongFixture();
    }

    private void insertSongFixture() {
        songId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        jdbc.update("INSERT INTO artists (id, spotify_id, name) VALUES (?, ?, ?)",
                artistId, "spotify_artist_sf", "Vangelis");
        jdbc.update("INSERT INTO albums (id, spotify_id, title, artist_id) VALUES (?, ?, ?, ?)",
                albumId, "spotify_album_sf", "Chariots of Fire", artistId);
        jdbc.update("INSERT INTO songs (id, spotify_id, title, album_id) VALUES (?, ?, ?, ?)",
                songId, "spotify_song_sf", "Chariots of Fire", albumId);
        jdbc.update("INSERT INTO song_artists (song_id, artist_id) VALUES (?, ?)", songId, artistId);
    }

    private String registerAndLogin() throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"wara","email":"wara@ecos.es","password":"password123","language":"es"}
                        """));
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wara@ecos.es","password":"password123"}
                                """))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    void getById_existingSong_returns200WithTitleAndArtist() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/songs/" + songId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(songId.toString()))
                .andExpect(jsonPath("$.title").value("Chariots of Fire"))
                .andExpect(jsonPath("$.artists[0]").value("Vangelis"));
    }

    @Test
    void getById_nonExistingSong_returns404() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/songs/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void getRelated_existingSong_returns200WithRelatedSongs() throws Exception {
        String token = registerAndLogin();
        Song related = new Song();
        related.setId(UUID.randomUUID());
        related.setTitle("Related Song");
        when(musicProvider.getSimilarSongs(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(related));

        mockMvc.perform(get("/v1/songs/" + songId + "/related")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Related Song"));
    }

    @Test
    void getAudioFeatures_songWithoutFeatures_returns404() throws Exception {
        String token = registerAndLogin();

        // La canción existe pero no tiene audio features insertadas → 404
        mockMvc.perform(get("/v1/songs/" + songId + "/audio-features")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSong_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/songs/" + songId))
                .andExpect(status().isUnauthorized());
    }

}