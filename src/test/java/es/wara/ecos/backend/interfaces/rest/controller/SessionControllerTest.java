package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.session.AddNodeToSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.CreateExplorationSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionByIdUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionHistoryUseCase;
import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.SessionNode;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import es.wara.ecos.backend.infrastructure.security.RateLimitFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SessionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    RateLimitFilter rateLimitFilter;

    @MockitoBean
    GetSessionByIdUseCase getSessionByIdUseCase;

    @MockitoBean
    CreateExplorationSessionUseCase createSessionUseCase;

    @MockitoBean
    GetSessionHistoryUseCase getSessionHistoryUseCase;

    @MockitoBean
    AddNodeToSessionUseCase addNodeToSessionUseCase;

    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SONG_ID    = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID SONG_20    = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000999");

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private ExplorationSession buildSession(UUID id, UUID userId) {
        return new ExplorationSession(id, userId, SONG_ID, LocalDateTime.now(), new ArrayList<>());
    }

    @Test
    void create_validBody_returns201() throws Exception {
        ExplorationSession session = buildSession(SESSION_ID, USER_ID);
        when(createSessionUseCase.execute(USER_ID, SONG_ID)).thenReturn(session);

        mockMvc.perform(post("/v1/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"startSongId": "00000000-0000-0000-0000-000000000010"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    @Test
    void create_missingStartSongId_returns400() throws Exception {
        mockMvc.perform(post("/v1/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByUser_returns200WithList() throws Exception {
        when(getSessionHistoryUseCase.execute(USER_ID)).thenReturn(List.of(buildSession(SESSION_ID, USER_ID)));

        mockMvc.perform(get("/v1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value(USER_ID.toString()));
    }

    @Test
    void getById_existingSession_returns200() throws Exception {
        when(getSessionByIdUseCase.execute(SESSION_ID)).thenReturn(buildSession(SESSION_ID, USER_ID));

        mockMvc.perform(get("/v1/sessions/" + SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()));
    }

    @Test
    void getById_nonExistingSession_returns404() throws Exception {
        when(getSessionByIdUseCase.execute(UNKNOWN_ID))
                .thenThrow(new IllegalArgumentException("Sesión no encontrada: " + UNKNOWN_ID));

        mockMvc.perform(get("/v1/sessions/" + UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void addNode_validBody_returns200WithNode() throws Exception {
        ExplorationSession session = buildSession(SESSION_ID, USER_ID);
        Song song = new Song();
        song.setId(SONG_20);
        SessionNode node = new SessionNode();
        node.setSong(song);
        node.setSequenceOrder(1);
        session.setNodes(List.of(node));

        when(addNodeToSessionUseCase.execute(eq(SESSION_ID), eq(SONG_20), any(), any())).thenReturn(session);

        mockMvc.perform(post("/v1/sessions/" + SESSION_ID + "/nodes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"songId": "00000000-0000-0000-0000-000000000020", "x": 1.0, "y": 2.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.nodes[0].sequenceOrder").value(1));
    }

}
