package es.wara.ecos.backend.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.wara.ecos.backend.infrastructure.persistence.entity.UserInteractionEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaUserRepository;
import es.wara.ecos.backend.infrastructure.persistence.repository.UserInteractionRepositoryImpl;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import es.wara.ecos.backend.infrastructure.security.RateLimitFilter;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.RecordInteractionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InteractionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InteractionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean JwtAuthFilter jwtAuthFilter;
    @MockitoBean RateLimitFilter rateLimitFilter;
    @MockitoBean UserInteractionRepositoryImpl interactionRepository;
    @MockitoBean JpaSongRepository jpaSongRepository;
    @MockitoBean JpaUserRepository jpaUserRepository;

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID SONG_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        when(interactionRepository.save(any())).thenReturn(new UserInteractionEntity());
    }

    @Test
    void record_playAction_returns204() throws Exception {
        RecordInteractionRequest request = new RecordInteractionRequest(SONG_ID, "play", 45000);

        try (MockedStatic<SecurityUtils> secUtils = Mockito.mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

            mockMvc.perform(post("/v1/interactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(interactionRepository).save(any(UserInteractionEntity.class));
        }
    }

    @Test
    void record_skipAction_returns204() throws Exception {
        RecordInteractionRequest request = new RecordInteractionRequest(SONG_ID, "skip", null);

        try (MockedStatic<SecurityUtils> secUtils = Mockito.mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

            mockMvc.perform(post("/v1/interactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void record_invalidAction_returns400() throws Exception {
        RecordInteractionRequest request = new RecordInteractionRequest(SONG_ID, "LIKE", null);

        try (MockedStatic<SecurityUtils> secUtils = Mockito.mockStatic(SecurityUtils.class)) {
            secUtils.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

            mockMvc.perform(post("/v1/interactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void record_missingSongId_returns400() throws Exception {
        String json = """
                {"action":"play","durationMs":1000}
                """;

        mockMvc.perform(post("/v1/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void record_missingAction_returns400() throws Exception {
        String json = """
                {"songId":"%s","durationMs":1000}
                """.formatted(SONG_ID);

        mockMvc.perform(post("/v1/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}