package es.wara.ecos.backend.functional;

import es.wara.ecos.backend.support.BaseFunctionalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests funcionales del flujo de sesiones de exploración.
 *
 * Las sesiones requieren una canción existente en la BD. Como no se puede
 * llamar a APIs externas en tests funcionales, la canción se inserta directamente
 * con JdbcTemplate como fixture de test.
 */
class SessionFlowTest extends BaseFunctionalTest {

    @Autowired
    JdbcTemplate jdbc;

    private UUID songId;

    @BeforeEach
    void cleanDb() {
        // CASCADE propaga el truncado a todas las tablas dependientes
        jdbc.execute("TRUNCATE TABLE artists CASCADE");
        jdbc.execute("TRUNCATE TABLE users CASCADE");
        insertSongFixture();
    }

    /**
     * Inserta un artista, álbum y canción mínimos para poder crear sesiones.
     * Los tests de sesión necesitan un startSongId válido — este es el fixture más
     * sencillo que satisface las FK de la tabla sessions.
     */
    private void insertSongFixture() {
        songId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();

        jdbc.update("INSERT INTO artists (id, spotify_id, name) VALUES (?, ?, ?)",
                artistId, "spotify_artist_test", "Artist Test");
        jdbc.update("INSERT INTO albums (id, spotify_id, title, artist_id) VALUES (?, ?, ?, ?)",
                albumId, "spotify_album_test", "Album Test", artistId);
        jdbc.update("INSERT INTO songs (id, spotify_id, title, album_id) VALUES (?, ?, ?, ?)",
                songId, "spotify_song_test", "Song Test", albumId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

    private String createSession(String token, UUID startSongId) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"startSongId":"%s"}
                                """.formatted(startSongId)))
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
                                {"startSongId":"%s"}
                                """.formatted(songId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.startSongId").value(songId.toString()))
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.nodes").isEmpty());
    }

    @Test
    void createSession_nonExistingSong_returns404() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        UUID fakeSongId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"startSongId":"%s"}
                                """.formatted(fakeSongId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void createSession_missingStartSongId_returns400() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(post("/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{}"))
                .andExpect(status().isBadRequest());
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
        createSession(token, songId);

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
        String sessionId = createSession(token, songId);

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
    void addNode_validSong_returns200WithNodeInSession() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        String sessionId = createSession(token, songId);

        mockMvc.perform(post("/v1/sessions/" + sessionId + "/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"songId":"%s","x":1.5,"y":2.5}
                                """.formatted(songId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.nodes.length()").value(1))
                .andExpect(jsonPath("$.nodes[0].song.id").value(songId.toString()));
    }

    @Test
    void addNode_missingSongId_returns400() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        String sessionId = createSession(token, songId);

        mockMvc.perform(post("/v1/sessions/" + sessionId + "/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"x":1.5,"y":2.5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sessions_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/sessions"))
                .andExpect(status().isUnauthorized());
    }
}