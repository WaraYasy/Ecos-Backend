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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_returns200WithAuthUrl() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, List.of()));

        when(spotifyAuthPort.getAuthorizationUrl(1L))
                .thenReturn("https://accounts.spotify.com/authorize?client_id=test&state=1");

        mockMvc.perform(get("/v1/auth/spotify/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.url").value(containsString("accounts.spotify.com")));
    }

    @Test
    void callback_validCodeAndState_returns200WithConfirmation() throws Exception {
        doNothing().when(spotifyAuthPort).exchangeCodeForToken("abc", "1");

        mockMvc.perform(get("/callback/spotify")
                        .param("code", "abc")
                        .param("state", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cuenta Spotify vinculada"));
    }

}
