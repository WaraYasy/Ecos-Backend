package es.wara.ecos.backend.functional;

import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
import es.wara.ecos.backend.domain.port.SpotifyPlaylistPort;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests funcionales para el endpoint de exportación de playlists a Spotify.
 *
 * SpotifyAuthPort y SpotifyPlaylistPort están mockeados para evitar llamadas
 * reales a la API de Spotify. El flujo completo (registro, login, crear playlist,
 * exportar) se ejecuta contra PostgreSQL real (Testcontainers).
 */
class SpotifyExportFlowTest extends BaseFunctionalTest {

    // Prevents MusicProviderAdapter from being instantiated (it injects SpotifyClient
    // as a concrete type, which would conflict with @MockitoBean SpotifyPlaylistPort)
    @MockitoBean
    MusicProvider musicProvider;

    @MockitoBean
    SpotifyAuthPort spotifyAuthPort;

    @MockitoBean
    SpotifyPlaylistPort spotifyPlaylistPort;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE TABLE artists, users CASCADE");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private record LoginResult(String token, UUID userId) {}

    private LoginResult registerAndLogin(String username, String email) throws Exception {
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

        String token  = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        UUID   userId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("userId").asText());
        return new LoginResult(token, userId);
    }

    private String createPlaylist(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"name":"Mi playlist ECOS","description":"Para exportar"}
                                """))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void exportToSpotify_linkedUser_returns200WithPlaylistId() throws Exception {
        LoginResult login = registerAndLogin("wara", "wara@ecos.es");
        String playlistId = createPlaylist(login.token());

        when(spotifyAuthPort.getTokenForUser(login.userId())).thenReturn("spotify-user-token");
        when(spotifyPlaylistPort.getCurrentUserId("spotify-user-token")).thenReturn("spotify-uid");
        when(spotifyPlaylistPort.createPlaylist(
                eq("spotify-user-token"), eq("spotify-uid"), anyString(), anyString()))
                .thenReturn("spotify-playlist-abc");
        doNothing().when(spotifyPlaylistPort).addTracksToPlaylist(anyString(), anyString(), any(List.class));

        mockMvc.perform(post("/v1/playlists/" + playlistId + "/export/spotify")
                        .header("Authorization", "Bearer " + login.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playlistId));
    }

    @Test
    void exportToSpotify_userNotLinkedToSpotify_returns404() throws Exception {
        LoginResult login = registerAndLogin("wara", "wara@ecos.es");
        String playlistId = createPlaylist(login.token());

        when(spotifyAuthPort.getTokenForUser(login.userId()))
                .thenThrow(new IllegalArgumentException("El usuario no ha vinculado su cuenta de Spotify"));

        mockMvc.perform(post("/v1/playlists/" + playlistId + "/export/spotify")
                        .header("Authorization", "Bearer " + login.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void exportToSpotify_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/v1/playlists/00000000-0000-0000-0000-000000000001/export/spotify"))
                .andExpect(status().isUnauthorized());
    }
}