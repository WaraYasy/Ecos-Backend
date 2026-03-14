package es.wara.ecos.backend.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.wara.ecos.backend.application.usecase.user.CalculateUserProfileUseCase;
import es.wara.ecos.backend.application.usecase.user.GetUserProfileUseCase;
import es.wara.ecos.backend.application.usecase.user.RegisterUserUseCase;
import es.wara.ecos.backend.application.usecase.user.UpdateUserPreferencesUseCase;
import es.wara.ecos.backend.domain.exception.ConflictException;
import es.wara.ecos.backend.domain.model.MusicProfile;
import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    GetUserProfileUseCase getUserProfileUseCase;

    @MockitoBean
    UpdateUserPreferencesUseCase updateUserPreferencesUseCase;

    @MockitoBean
    CalculateUserProfileUseCase calculateUserProfileUseCase;

    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000999");

    private static final User USER = new User(USER_ID, "wara", "wara@ecos.es", "es", null, LocalDateTime.now());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_validBody_returns201WithUserResponse() throws Exception {
        when(registerUserUseCase.execute("wara", "wara@ecos.es", "pass123", "es")).thenReturn(USER);

        mockMvc.perform(post("/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username": "wara", "email": "wara@ecos.es", "password": "pass123", "language": "es"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.username").value("wara"))
                .andExpect(jsonPath("$.email").value("wara@ecos.es"));
    }

    @Test
    void register_missingUsername_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email": "wara@ecos.es", "password": "pass123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        when(registerUserUseCase.execute(any(), any(), any(), any()))
                .thenThrow(new ConflictException("El nombre de usuario ya está en uso: wara"));

        mockMvc.perform(post("/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username": "wara", "email": "x@x.es", "password": "pass123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void getById_existingUser_returns200() throws Exception {
        when(getUserProfileUseCase.execute(USER_ID)).thenReturn(USER);

        mockMvc.perform(get("/v1/users/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("wara"));
    }

    @Test
    void getById_nonExistingUser_returns404() throws Exception {
        when(getUserProfileUseCase.execute(UNKNOWN_ID))
                .thenThrow(new IllegalArgumentException("Usuario no encontrado: " + UNKNOWN_ID));

        mockMvc.perform(get("/v1/users/" + UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void updatePreferences_ownUser_returns200WithUpdatedLanguage() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null, List.of()));

        User updated = new User(USER_ID, "wara", "wara@ecos.es", "en", null, LocalDateTime.now());
        when(updateUserPreferencesUseCase.execute(eq(USER_ID), eq("en"))).thenReturn(updated);

        mockMvc.perform(put("/v1/users/" + USER_ID + "/preferences")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"language": "en"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    void getMusicProfile_returns200WithProfile() throws Exception {
        MusicProfile profile = new MusicProfile(
                USER_ID,
                new BigDecimal("0.5"),
                new BigDecimal("0.3"),
                new BigDecimal("0.4"),
                Map.of("1980s", 0.7),
                List.of("rock", "pop")
        );
        when(calculateUserProfileUseCase.execute(USER_ID)).thenReturn(profile);

        mockMvc.perform(get("/v1/users/" + USER_ID + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.topGenres[0]").value("rock"));
    }

}
