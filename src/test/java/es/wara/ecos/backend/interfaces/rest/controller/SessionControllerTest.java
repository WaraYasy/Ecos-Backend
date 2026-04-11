package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.session.CreateExplorationSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.DeleteSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionByIdUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionHistoryUseCase;
import es.wara.ecos.backend.domain.exception.ConflictException;
import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.ExplorationSession;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SessionControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean RateLimitFilter rateLimitFilter;
    @MockitoBean CreateExplorationSessionUseCase createSessionUseCase;
    @MockitoBean DeleteSessionUseCase deleteSessionUseCase;
    @MockitoBean GetSessionByIdUseCase getSessionByIdUseCase;
    @MockitoBean GetSessionHistoryUseCase getSessionHistoryUseCase;

    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID SONG_A     = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @BeforeEach
    void setUpSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null, List.of()));
    }

    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    private ExplorationSession session(String name) {
        return new ExplorationSession(SESSION_ID, USER_ID, SONG_A, name, LocalDateTime.now(), List.of());
    }

    @Test
    void create_validRequest_returns201WithSession() throws Exception {
        when(createSessionUseCase.execute(eq(USER_ID), eq(SONG_A), any(), eq("Tarde de jazz")))
                .thenReturn(session("Tarde de jazz"));

        mockMvc.perform(post("/v1/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "startSongId": "%s",
                                  "songIds": ["%s"],
                                  "name": "Tarde de jazz"
                                }
                                """.formatted(SONG_A, SONG_A)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tarde de jazz"))
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()));
    }

    @Test
    void create_userHasMaxSessions_returns409() throws Exception {
        when(createSessionUseCase.execute(any(), any(), any(), any()))
                .thenThrow(new ConflictException("Ya tienes 2 sesiones guardadas."));

        mockMvc.perform(post("/v1/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"startSongId":"%s","songIds":["%s"]}
                                """.formatted(SONG_A, SONG_A)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void create_missingSongIds_returns400() throws Exception {
        mockMvc.perform(post("/v1/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"startSongId":"%s"}
                                """.formatted(SONG_A)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByUser_returnsSessions() throws Exception {
        when(getSessionHistoryUseCase.execute(USER_ID)).thenReturn(List.of(session("Jazz")));

        mockMvc.perform(get("/v1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Jazz"));
    }

    @Test
    void getById_existingSession_returns200() throws Exception {
        when(getSessionByIdUseCase.execute(SESSION_ID)).thenReturn(session("Rock 70s"));

        mockMvc.perform(get("/v1/sessions/" + SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rock 70s"));
    }

    @Test
    void getById_nonExisting_returns404() throws Exception {
        when(getSessionByIdUseCase.execute(SESSION_ID))
                .thenThrow(new IllegalArgumentException("Sesión no encontrada"));

        mockMvc.perform(get("/v1/sessions/" + SESSION_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_ownSession_returns204() throws Exception {
        doNothing().when(deleteSessionUseCase).execute(SESSION_ID, USER_ID);

        mockMvc.perform(delete("/v1/sessions/" + SESSION_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_foreignSession_returns403() throws Exception {
        doThrow(new ForbiddenException("No tienes permiso"))
                .when(deleteSessionUseCase).execute(SESSION_ID, USER_ID);

        mockMvc.perform(delete("/v1/sessions/" + SESSION_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

}
