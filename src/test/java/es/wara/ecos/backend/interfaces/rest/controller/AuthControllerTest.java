package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.user.LoginUseCase;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import es.wara.ecos.backend.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    LoginUseCase loginUseCase;

    @MockitoBean
    JwtService jwtService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void login_validCredentials_returns200WithTokenAndUserId() throws Exception {
        when(loginUseCase.execute("user@ecos.es", "secret123")).thenReturn(USER_ID);
        when(jwtService.generateToken(USER_ID)).thenReturn("jwt-token");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email": "user@ecos.es", "password": "secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    @Test
    void login_wrongCredentials_returns401() throws Exception {
        when(loginUseCase.execute("user@ecos.es", "wrong"))
                .thenThrow(new IllegalStateException("Credenciales incorrectas"));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email": "user@ecos.es", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_malformedJson_returns400Not500() throws Exception {
        // Regresión: JSON con escape inválido caía al handler 500 en vez de devolver 400
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@ecos.es\",\"password\":\"bad\\!pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

}