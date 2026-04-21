package es.wara.ecos.backend.functional;

import es.wara.ecos.backend.support.BaseFunctionalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests funcionales del flujo de sesiones de exploración.
 *
 * Una sesión agrupa una lista ordenada de canciones. Se crea de una vez con
 * startSongId + songIds. El usuario puede tener como máximo 2 sesiones.
 */
class SessionFlowTest extends BaseFunctionalTest {

    @Autowired
    JdbcTemplate jdbc;

    private UUID songId;
    private UUID songId2;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE TABLE artists, users CASCADE");
        insertSongFixtures();
    }

    private void insertSongFixtures() {
        songId  = UUID.randomUUID();
        songId2 = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        UUID albumId  = UUID.randomUUID();

        jdbc.update("INSERT INTO artists (id, spotify_id, name) VALUES (?, ?, ?)",
                artistId, "spotify_artist_test", "Artist Test");
        jdbc.update("INSERT INTO albums (id, spotify_id, title, artist_id) VALUES (?, ?, ?, ?)",
                albumId, "spotify_album_test", "Album Test", artistId);
        jdbc.update("INSERT INTO songs (id, spotify_id, title, album_id) VALUES (?, ?, ?, ?)",
                songId, "spotify_song_test", "Song Test", albumId);
        jdbc.update("INSERT INTO songs (id, spotify_id, title, album_id) VALUES (?, ?, ?, ?)",
                songId2, "spotify_song_test_2", "Song Test 2", albumId);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String username, String email) throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","password":"password123","language":"es"}
                        """.formatted(username, email)));

        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private String createSession(String token, UUID startSongId, UUID... extraSongIds) throws Exception {
        StringBuilder ids = new StringBuilder("\"" + startSongId + "\"");
        for (UUID id : extraSongIds) {
            ids.append(",\"").append(id).append("\"");
        }
        MvcResult result = mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"startSongId":"%s","songIds":[%s]}
                                """.formatted(startSongId, ids)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void createSession_authenticated_returns201WithData() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"startSongId":"%s","songIds":["%s","%s"]}
                                """.formatted(songId, songId, songId2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.startSongId").value(songId.toString()))
                .andExpect(jsonPath("$.songs").isArray())
                .andExpect(jsonPath("$.songs.length()").value(2));
    }

    @Test
    void createSession_withName_returnsName() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"startSongId":"%s","songIds":["%s"],"name":"Mi sesión de jazz"}
                                """.formatted(songId, songId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mi sesión de jazz"));
    }

    @Test
    void createSession_nonExistingStartSong_returns404() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        UUID fakeSongId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"startSongId":"%s","songIds":["%s"]}
                                """.formatted(fakeSongId, fakeSongId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void createSession_missingSongIds_returns400() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"startSongId":"%s"}
                                """.formatted(songId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSession_thirdSession_returns409() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        createSession(token, songId, songId2);
        createSession(token, songId2, songId);

        mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"startSongId":"%s","songIds":["%s"]}
                                """.formatted(songId, songId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void getSessions_emptyState_returnsEmptyList() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(get("/v1/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSessions_afterCreation_returnsListWithOne() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        createSession(token, songId, songId2);

        mockMvc.perform(get("/v1/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startSongId").value(songId.toString()));
    }

    @Test
    void getSessionById_existingSession_returns200() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        String sessionId = createSession(token, songId, songId2);

        mockMvc.perform(get("/v1/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.startSongId").value(songId.toString()));
    }

    @Test
    void getSessionById_nonExisting_returns404() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(get("/v1/sessions/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void deleteSession_ownSession_returns204() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        String sessionId = createSession(token, songId, songId2);

        mockMvc.perform(delete("/v1/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // verificar que ya no existe
        mockMvc.perform(get("/v1/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSession_foreignSession_returns403() throws Exception {
        String tokenA = registerAndLogin("wara",  "wara@ecos.es");
        String tokenB = registerAndLogin("other", "other@ecos.es");
        String sessionId = createSession(tokenA, songId, songId2);

        mockMvc.perform(delete("/v1/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void sessions_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/sessions"))
                .andExpect(status().isUnauthorized());
    }
}