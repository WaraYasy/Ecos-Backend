package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpotifyAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class SpotifyAuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    SpotifyAuthPort spotifyAuthPort;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_returns200WithAuthUrl() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null, List.of()));

        when(spotifyAuthPort.getAuthorizationUrl(USER_ID))
                .thenReturn("https://accounts.spotify.com/authorize?client_id=test&state=1");

        mockMvc.perform(get("/v1/auth/spotify/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.url").value(containsString("accounts.spotify.com")));
    }

    @Test
    void callback_validCodeAndState_returns200WithConfirmation() throws Exception {
        doNothing().when(spotifyAuthPort).exchangeCodeForToken("abc", "test-state-uuid");

        mockMvc.perform(get("/callback/spotify")
                        .param("code", "abc")
                        .param("state", "test-state-uuid"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cuenta Spotify vinculada"));
    }

}
