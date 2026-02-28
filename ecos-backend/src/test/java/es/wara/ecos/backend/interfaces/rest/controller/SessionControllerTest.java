package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.session.AddNodeToSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.CreateExplorationSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionHistoryUseCase;
import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.SessionNode;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SessionRepository sessionRepository;

    @MockitoBean
    CreateExplorationSessionUseCase createSessionUseCase;

    @MockitoBean
    GetSessionHistoryUseCase getSessionHistoryUseCase;

    @MockitoBean
    AddNodeToSessionUseCase addNodeToSessionUseCase;

    private ExplorationSession buildSession(Long id, Long userId) {
        return new ExplorationSession(id, userId, 10L, LocalDateTime.now(), new ArrayList<>());
    }

    @Test
    void create_validBody_returns201() throws Exception {
        ExplorationSession session = buildSession(1L, 5L);
        when(createSessionUseCase.execute(5L, 10L)).thenReturn(session);

        mockMvc.perform(post("/v1/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"userId": 5, "startSongId": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(5));
    }

    @Test
    void create_missingUserId_returns400() throws Exception {
        mockMvc.perform(post("/v1/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"startSongId": 10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByUser_returns200WithList() throws Exception {
        when(getSessionHistoryUseCase.execute(5L)).thenReturn(List.of(buildSession(1L, 5L)));

        mockMvc.perform(get("/v1/sessions").param("userId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value(5));
    }

    @Test
    void getById_existingSession_returns200() throws Exception {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(buildSession(1L, 5L)));

        mockMvc.perform(get("/v1/sessions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_nonExistingSession_returns404() throws Exception {
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/sessions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void addNode_validBody_returns200WithNode() throws Exception {
        ExplorationSession session = buildSession(1L, 5L);
        Song song = new Song();
        song.setId(20L);
        SessionNode node = new SessionNode();
        node.setSong(song);
        node.setSequenceOrder(1);
        session.setNodes(List.of(node));

        when(addNodeToSessionUseCase.execute(eq(1L), eq(20L), any(), any())).thenReturn(session);

        mockMvc.perform(post("/v1/sessions/1/nodes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"songId": 20, "x": 1.0, "y": 2.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.nodes[0].sequenceOrder").value(1));
    }

}
