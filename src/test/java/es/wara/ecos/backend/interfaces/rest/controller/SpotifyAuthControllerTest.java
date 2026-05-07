package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
import es.wara.ecos.backend.infrastructure.security.JwtAuthFilter;
import es.wara.ecos.backend.infrastructure.security.RateLimitFilter;
import es.wara.ecos.backend.interfaces.rest.dto.LoginResponse;
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
import java.util.Optional;
import java.util.UUID;

import es.wara.ecos.backend.domain.exception.ConflictException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    RateLimitFilter rateLimitFilter;

    @MockitoBean
    SpotifyAuthPort spotifyAuthPort;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginWithSpotify_public_returns200WithUrl() throws Exception {
        when(spotifyAuthPort.getLoginUrl())
                .thenReturn("https://accounts.spotify.com/authorize?client_id=test&state=abc");

        mockMvc.perform(get("/v1/auth/spotify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(containsString("accounts.spotify.com")));
    }

    @Test
    void linkSpotify_authenticated_returns200WithUrl() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null, List.of()));

        when(spotifyAuthPort.getAuthorizationUrl(USER_ID))
                .thenReturn("https://accounts.spotify.com/authorize?client_id=test&state=1");

        mockMvc.perform(get("/v1/auth/spotify/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(containsString("accounts.spotify.com")));
    }

    @Test
    void callback_linkFlow_redirectsToDeepLinkWithLinkedTrue() throws Exception {
        when(spotifyAuthPort.exchangeCodeForToken("abc", "link-state"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/callback/spotify")
                        .param("code", "abc")
                        .param("state", "link-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "ecos://auth/callback?linked=true"));
    }

    @Test
    void callback_loginFlow_redirectsToDeepLink() throws Exception {
        LoginResponse login = new LoginResponse("jwt-token-123", USER_ID, false);
        when(spotifyAuthPort.exchangeCodeForToken("abc", "login-state"))
                .thenReturn(Optional.of(login));

        mockMvc.perform(get("/callback/spotify")
                        .param("code", "abc")
                        .param("state", "login-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("ecos://auth/callback")))
                .andExpect(header().string("Location", containsString("token=jwt-token-123")));
    }

    @Test
    void callback_loginFlow_linkedAccount_includesLinkedParamInDeepLink() throws Exception {
        LoginResponse login = new LoginResponse("jwt-token-123", USER_ID, true);
        when(spotifyAuthPort.exchangeCodeForToken("abc", "login-state"))
                .thenReturn(Optional.of(login));

        mockMvc.perform(get("/callback/spotify")
                        .param("code", "abc")
                        .param("state", "login-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("linked=true")));
    }

    @Test
    void callback_invalidOrExpiredState_returns404() throws Exception {
        doThrow(new IllegalArgumentException("Estado OAuth inválido o expirado"))
                .when(spotifyAuthPort).exchangeCodeForToken("code", "bad-state");

        mockMvc.perform(get("/callback/spotify")
                        .param("code", "code")
                        .param("state", "bad-state"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ── DELETE /v1/auth/spotify ──────────────────────────────────────────────

    @Test
    void unlinkSpotify_authenticated_returns204() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null, List.of()));
        doNothing().when(spotifyAuthPort).unlinkSpotify(USER_ID);

        mockMvc.perform(delete("/v1/auth/spotify"))
                .andExpect(status().isNoContent());
    }

    @Test
    void unlinkSpotify_noPassword_returns409() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null, List.of()));
        doThrow(new ConflictException("Establece una contraseña antes de desvincular Spotify"))
                .when(spotifyAuthPort).unlinkSpotify(USER_ID);

        mockMvc.perform(delete("/v1/auth/spotify"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void unlinkSpotify_notLinked_returns404() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID.toString(), null, List.of()));
        doThrow(new IllegalArgumentException("El usuario no tiene una cuenta de Spotify vinculada"))
                .when(spotifyAuthPort).unlinkSpotify(USER_ID);

        mockMvc.perform(delete("/v1/auth/spotify"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}