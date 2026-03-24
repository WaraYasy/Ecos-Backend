package es.wara.ecos.backend.functional;

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
 * Tests funcionales para el endpoint de búsqueda.
 *
 * MusicProvider está mockeado para evitar llamadas externas cuando BD está vacía.
 * Los tests de búsqueda BD-positiva insertan datos directamente para verificar
 * que la especificación JPA devuelve los resultados correctos.
 */
class SearchFlowTest extends BaseFunctionalTest {

    @MockitoBean
    MusicProvider musicProvider;

    @Autowired
    JdbcTemplate jdbc;

    private UUID songId;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE TABLE artists, users CASCADE");
        insertSongFixture();
        // Prevenir llamadas reales en tests que no las necesitan
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());
        when(musicProvider.searchSongsByTag(anyString(), anyInt())).thenReturn(List.of());
        when(musicProvider.getSoundtrackSongs(anyString())).thenReturn(List.of());
    }

    private void insertSongFixture() {
        songId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        jdbc.update("INSERT INTO artists (id, spotify_id, name) VALUES (?, ?, ?)",
                artistId, "spotify_artist_search", "Vangelis");
        jdbc.update("INSERT INTO albums (id, spotify_id, title, artist_id) VALUES (?, ?, ?, ?)",
                albumId, "spotify_album_search", "Chariots of Fire", artistId);
        jdbc.update("INSERT INTO songs (id, spotify_id, title, album_id) VALUES (?, ?, ?, ?)",
                songId, "spotify_song_search", "Chariots of Fire", albumId);
        jdbc.update("INSERT INTO song_artists (song_id, artist_id) VALUES (?, ?)", songId, artistId);
        jdbc.update("INSERT INTO song_tags (song_id, tag) VALUES (?, ?)", songId, "dark academia");
        jdbc.update("INSERT INTO song_tags (song_id, tag) VALUES (?, ?)", songId, "indie rock");
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
    void search_queryMatchesTitle_returns200WithSong() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/search")
                        .param("q", "Chariots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Chariots of Fire"));
    }

    @Test
    void search_queryMatchesArtistName_returns200WithSong() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/search")
                        .param("q", "Vangelis")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Chariots of Fire"));
    }

    @Test
    void search_noMatchInBd_returnsEmptyList() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/search")
                        .param("q", "XyzNoExiste999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void search_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/search").param("q", "Vangelis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_byAesthetic_doesNotThrow500() throws Exception {
        // Regresión: Subquery<Long> en SongSpecification causaba
        // "Cannot compare UUID with Long" al filtrar por aesthetic
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/search")
                        .param("aesthetic", "dark academia")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Chariots of Fire"));
    }

    @Test
    void search_byGenreExactMatch_returnsSong() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/search")
                        .param("genre", "indie rock")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Chariots of Fire"));
    }

    @Test
    void search_byGenrePartialMatch_returnsSong() throws Exception {
        // "indie" debe hacer match parcial sobre el tag "indie rock"
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/search")
                        .param("genre", "indie")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Chariots of Fire"));
    }

    @Test
    void search_byGenreNoMatch_returnsEmptyList() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/search")
                        .param("genre", "heavy metal")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void search_byGenreAndAesthetic_andLogicReturnsSong() throws Exception {
        // La canción tiene los dos tags — debe aparecer con ambos filtros
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/search")
                        .param("genre", "indie")
                        .param("aesthetic", "dark academia")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Chariots of Fire"));
    }

    @Test
    void search_byGenreAndAesthetic_andLogicReturnsEmpty_whenOnlyOneTagMatches() throws Exception {
        // Canción tiene "indie rock" pero no tiene aesthetic "cottagecore" → sin resultados
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/search")
                        .param("genre", "indie")
                        .param("aesthetic", "cottagecore")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

}