package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.playlist.CreatePlaylistUseCase;
import es.wara.ecos.backend.domain.exception.ConflictException;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import es.wara.ecos.backend.domain.port.UserRepository;
import es.wara.ecos.backend.infrastructure.adapter.ai.RecommendationClient;
import es.wara.ecos.backend.infrastructure.adapter.ai.RecommendationClient.CrossoverResult;
import es.wara.ecos.backend.infrastructure.adapter.ai.RecommendationClient.ScoredSongId;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import es.wara.ecos.backend.infrastructure.security.RateLimitFilter;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean RateLimitFilter rateLimitFilter;
    @MockitoBean RecommendationClient recommendationClient;
    @MockitoBean SongRepository songRepository;
    @MockitoBean UserRepository userRepository;
    @MockitoBean PlaylistRepository playlistRepository;
    @MockitoBean CreatePlaylistUseCase createPlaylistUseCase;

    private static final UUID USER_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID USER_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID SONG_1 = UUID.fromString("11111111-0000-0000-0000-000000000011");
    private static final UUID SONG_2 = UUID.fromString("22222222-0000-0000-0000-000000000022");

    private Song song(UUID id, String title) {
        Song s = new Song();
        s.setId(id);
        s.setTitle(title);
        s.setArtists(List.of());
        return s;
    }

    // ─── GET /v1/recommendations ──────────────────────────────────────────────

    @Test
    void getRecommendations_hasSongs_returns200WithList() throws Exception {
        when(songRepository.findById(SONG_1)).thenReturn(Optional.of(song(SONG_1, "Chariots of Fire")));
        when(songRepository.findById(SONG_2)).thenReturn(Optional.of(song(SONG_2, "Vangelis Theme")));

        try (MockedStatic<SecurityUtils> su = Mockito.mockStatic(SecurityUtils.class)) {
            su.when(SecurityUtils::getCurrentUserId).thenReturn(USER_A);
            when(recommendationClient.getRecommendations(USER_A)).thenReturn(List.of(
                    new ScoredSongId(SONG_1.toString(), 0.91),
                    new ScoredSongId(SONG_2.toString(), 0.85)
            ));

            mockMvc.perform(get("/v1/recommendations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].title").value("Chariots of Fire"));
        }
    }

    @Test
    void getRecommendations_microUnavailable_returnsEmptyList() throws Exception {
        try (MockedStatic<SecurityUtils> su = Mockito.mockStatic(SecurityUtils.class)) {
            su.when(SecurityUtils::getCurrentUserId).thenReturn(USER_A);
            when(recommendationClient.getRecommendations(USER_A)).thenReturn(List.of());

            mockMvc.perform(get("/v1/recommendations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Test
    void getRecommendations_noHistory_returns409() throws Exception {
        try (MockedStatic<SecurityUtils> su = Mockito.mockStatic(SecurityUtils.class)) {
            su.when(SecurityUtils::getCurrentUserId).thenReturn(USER_A);
            when(recommendationClient.getRecommendations(USER_A))
                    .thenThrow(new ConflictException("El usuario no tiene historial de interacciones suficiente"));

            mockMvc.perform(get("/v1/recommendations"))
                    .andExpect(status().isConflict());
        }
    }

    // ─── GET /v1/recommendations/crossover ───────────────────────────────────

    @Test
    void getCrossover_hasSongs_returns200WithOverlapCount() throws Exception {
        when(songRepository.findById(SONG_1)).thenReturn(Optional.of(song(SONG_1, "Shared Song")));

        try (MockedStatic<SecurityUtils> su = Mockito.mockStatic(SecurityUtils.class)) {
            su.when(SecurityUtils::getCurrentUserId).thenReturn(USER_A);
            when(recommendationClient.getCrossoverRecommendations(USER_A, USER_B))
                    .thenReturn(Optional.of(new CrossoverResult(
                            List.of(new ScoredSongId(SONG_1.toString(), 0.88)),
                            5
                    )));

            mockMvc.perform(get("/v1/recommendations/crossover").param("userBId", USER_B.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overlapCount").value(5))
                    .andExpect(jsonPath("$.songs").isArray())
                    .andExpect(jsonPath("$.songs[0].title").value("Shared Song"));
        }
    }

    @Test
    void getCrossover_microUnavailable_returns200WithEmptyResult() throws Exception {
        try (MockedStatic<SecurityUtils> su = Mockito.mockStatic(SecurityUtils.class)) {
            su.when(SecurityUtils::getCurrentUserId).thenReturn(USER_A);
            when(recommendationClient.getCrossoverRecommendations(any(), any()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/v1/recommendations/crossover").param("userBId", USER_B.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overlapCount").value(0))
                    .andExpect(jsonPath("$.songs").isArray())
                    .andExpect(jsonPath("$.songs.length()").value(0));
        }
    }

    @Test
    void getCrossover_missingUserBId_returns400() throws Exception {
        mockMvc.perform(get("/v1/recommendations/crossover"))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /v1/recommendations/crossover/save ─────────────────────────────

    private User userB() {
        User u = new User();
        u.setId(USER_B);
        u.setUsername("amigo");
        u.setUserCode("ECO-TEST-0001");
        return u;
    }

    private Playlist playlist(UUID id) {
        Playlist p = new Playlist();
        p.setId(id);
        p.setUserId(USER_A);
        p.setName("Crossover con amigo");
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    @Test
    void saveCrossover_validCode_returns201WithPlaylistInfo() throws Exception {
        UUID playlistId = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
        when(userRepository.findByUserCode("ECO-TEST-0001")).thenReturn(Optional.of(userB()));
        when(recommendationClient.getCrossoverRecommendations(USER_A, USER_B))
                .thenReturn(Optional.of(new CrossoverResult(
                        List.of(new ScoredSongId(SONG_1.toString(), 0.9)),
                        3
                )));
        when(songRepository.findById(SONG_1)).thenReturn(Optional.of(song(SONG_1, "Shared Song")));
        when(createPlaylistUseCase.execute(eq(USER_A), any(), any())).thenReturn(playlist(playlistId));

        try (MockedStatic<SecurityUtils> su = Mockito.mockStatic(SecurityUtils.class)) {
            su.when(SecurityUtils::getCurrentUserId).thenReturn(USER_A);

            mockMvc.perform(post("/v1/recommendations/crossover/save").param("userCode", "ECO-TEST-0001"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.playlistId").value(playlistId.toString()))
                    .andExpect(jsonPath("$.playlistName").value("Crossover con amigo"))
                    .andExpect(jsonPath("$.songCount").value(1));
        }

        verify(playlistRepository).addSong(eq(playlistId), eq(SONG_1));
    }

    @Test
    void saveCrossover_unknownCode_returns404() throws Exception {
        when(userRepository.findByUserCode("ECO-FAKE-9999")).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> su = Mockito.mockStatic(SecurityUtils.class)) {
            su.when(SecurityUtils::getCurrentUserId).thenReturn(USER_A);

            mockMvc.perform(post("/v1/recommendations/crossover/save").param("userCode", "ECO-FAKE-9999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void saveCrossover_selfCrossover_returns404() throws Exception {
        User self = new User();
        self.setId(USER_A);
        self.setUsername("yo");
        self.setUserCode("ECO-SELF-0001");
        when(userRepository.findByUserCode("ECO-SELF-0001")).thenReturn(Optional.of(self));

        try (MockedStatic<SecurityUtils> su = Mockito.mockStatic(SecurityUtils.class)) {
            su.when(SecurityUtils::getCurrentUserId).thenReturn(USER_A);

            mockMvc.perform(post("/v1/recommendations/crossover/save").param("userCode", "ECO-SELF-0001"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void saveCrossover_emptyProfile_returns404() throws Exception {
        when(userRepository.findByUserCode("ECO-TEST-0001")).thenReturn(Optional.of(userB()));
        when(recommendationClient.getCrossoverRecommendations(USER_A, USER_B))
                .thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> su = Mockito.mockStatic(SecurityUtils.class)) {
            su.when(SecurityUtils::getCurrentUserId).thenReturn(USER_A);

            mockMvc.perform(post("/v1/recommendations/crossover/save").param("userCode", "ECO-TEST-0001"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void saveCrossover_missingUserCode_returns400() throws Exception {
        mockMvc.perform(post("/v1/recommendations/crossover/save"))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /v1/users/by-code — seguridad: sin token devuelve 401 ───────────
    // (cubierto en UserControllerTest)
}