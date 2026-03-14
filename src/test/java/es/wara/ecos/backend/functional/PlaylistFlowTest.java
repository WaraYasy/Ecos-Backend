package es.wara.ecos.backend.functional;

import es.wara.ecos.backend.support.BaseFunctionalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests funcionales del flujo de playlists.
 *
 * Cubre creación, consulta, eliminación y protección IDOR contra acceso
 * a playlists de otros usuarios. Todos los tests usan JWT real.
 */
class PlaylistFlowTest extends BaseFunctionalTest {

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE TABLE users CASCADE");
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

    private String createPlaylist(String token, String name, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"name":"%s","description":"%s"}
                                """.formatted(name, description)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void createPlaylist_authenticated_returns201WithData() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(post("/v1/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"name":"Mi playlist","description":"Descripcion"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Mi playlist"))
                .andExpect(jsonPath("$.description").value("Descripcion"));
    }

    @Test
    void getUserPlaylists_emptyState_returnsEmptyList() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(get("/v1/playlists")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getUserPlaylists_afterCreation_returnsListWithOne() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        createPlaylist(token, "Mi playlist", "Desc");

        mockMvc.perform(get("/v1/playlists")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Mi playlist"));
    }

    @Test
    void getPlaylistById_existingPlaylist_returns200() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        String id = createPlaylist(token, "Mi playlist", "Desc");

        mockMvc.perform(get("/v1/playlists/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Mi playlist"));
    }

    @Test
    void getPlaylistById_nonExisting_returns404() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(get("/v1/playlists/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void deletePlaylist_ownPlaylist_returns204() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        String id = createPlaylist(token, "Mi playlist", "Desc");

        mockMvc.perform(delete("/v1/playlists/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePlaylist_afterDeletion_returns404() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        String id = createPlaylist(token, "Mi playlist", "Desc");

        mockMvc.perform(delete("/v1/playlists/" + id)
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/v1/playlists/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePlaylist_otherUsersPlaylist_returns403() throws Exception {
        String tokenOwner = registerAndLogin("wara", "wara@ecos.es");
        String tokenOther = registerAndLogin("otra", "otra@ecos.es");
        String id = createPlaylist(tokenOwner, "Playlist de wara", "Desc");

        mockMvc.perform(delete("/v1/playlists/" + id)
                        .header("Authorization", "Bearer " + tokenOther))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void createPlaylist_missingName_returns400() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(post("/v1/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"description":"Sin nombre"}
                                """))
                .andExpect(status().isBadRequest());
    }
}