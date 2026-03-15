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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests funcionales para añadir y eliminar canciones de playlists.
 *
 * Cubre la protección IDOR: solo el propietario puede modificar su playlist.
 * Las canciones se insertan directamente via JdbcTemplate como fixture.
 */
class PlaylistSongsFlowTest extends BaseFunctionalTest {

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
                artistId, "spotify_artist_psf", "Artist PSF");
        jdbc.update("INSERT INTO albums (id, spotify_id, title, artist_id) VALUES (?, ?, ?, ?)",
                albumId, "spotify_album_psf", "Album PSF", artistId);
        jdbc.update("INSERT INTO songs (id, spotify_id, title, album_id) VALUES (?, ?, ?, ?)",
                songId, "spotify_song_psf", "Song PSF", albumId);
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

    private String createPlaylist(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"name":"Test playlist","description":"Desc"}
                                """))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    // ── addSong ───────────────────────────────────────────────────────────────

    @Test
    void addSong_toOwnPlaylist_returns204() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        String playlistId = createPlaylist(token);

        mockMvc.perform(post("/v1/playlists/" + playlistId + "/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"songId":"%s"}
                                """.formatted(songId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void addSong_toOtherUsersPlaylist_returns403() throws Exception {
        String tokenOwner = registerAndLogin("wara", "wara@ecos.es");
        String tokenOther = registerAndLogin("otra", "otra@ecos.es");
        String playlistId = createPlaylist(tokenOwner);

        mockMvc.perform(post("/v1/playlists/" + playlistId + "/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenOther)
                        .content("""
                                {"songId":"%s"}
                                """.formatted(songId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void addSong_nonExistingSong_returns404() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        String playlistId = createPlaylist(token);
        UUID fakeSongId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        mockMvc.perform(post("/v1/playlists/" + playlistId + "/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"songId":"%s"}
                                """.formatted(fakeSongId)))
                .andExpect(status().isNotFound());
    }

    // ── removeSong ────────────────────────────────────────────────────────────

    @Test
    void removeSong_fromOwnPlaylist_returns204() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");
        String playlistId = createPlaylist(token);

        // Añadir la canción primero
        mockMvc.perform(post("/v1/playlists/" + playlistId + "/songs")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("""
                        {"songId":"%s"}
                        """.formatted(songId)));

        mockMvc.perform(delete("/v1/playlists/" + playlistId + "/songs/" + songId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeSong_fromOtherUsersPlaylist_returns403() throws Exception {
        String tokenOwner = registerAndLogin("wara", "wara@ecos.es");
        String tokenOther = registerAndLogin("otra", "otra@ecos.es");
        String playlistId = createPlaylist(tokenOwner);

        mockMvc.perform(delete("/v1/playlists/" + playlistId + "/songs/" + songId)
                        .header("Authorization", "Bearer " + tokenOther))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

}