package es.wara.ecos.backend.infrastructure.adapter.spotify;

import es.wara.ecos.backend.infrastructure.persistence.entity.OAuthStateEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SpotifyTokenEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaOAuthStateRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSpotifyTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de SpotifyAuthService.
 *
 * Cubre el flujo OAuth completo: generación de URL, intercambio de código,
 * validación de state CSRF, obtención de token y refresco automático.
 * RestTemplate, repositorios y TextEncryptor están mockeados (sin red ni BD real).
 */
@ExtendWith(MockitoExtension.class)
class SpotifyAuthServiceTest {

    @Mock RestTemplate restTemplate;
    @Mock JpaSpotifyTokenRepository tokenRepository;
    @Mock JpaOAuthStateRepository oAuthStateRepository;
    @Mock TextEncryptor encryptor;

    @InjectMocks
    SpotifyAuthService service;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void injectValues() {
        ReflectionTestUtils.setField(service, "clientId",     "test-client-id");
        ReflectionTestUtils.setField(service, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(service, "redirectUri",  "http://localhost/callback/spotify");
    }

    // ── getAuthorizationUrl ──────────────────────────────────────────────────

    @Test
    void getAuthorizationUrl_persistsStateAndReturnsUrlContainingClientId() {
        String url = service.getAuthorizationUrl(USER_ID);

        ArgumentCaptor<OAuthStateEntity> captor = ArgumentCaptor.forClass(OAuthStateEntity.class);
        verify(oAuthStateRepository).save(captor.capture());

        OAuthStateEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getState()).isNotNull();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());

        assertThat(url).contains("accounts.spotify.com/authorize");
        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("state=" + saved.getState());
    }

    // ── exchangeCodeForToken ─────────────────────────────────────────────────

    @Test
    void exchangeCodeForToken_validState_callsSpotifyAndPersistsToken() {
        OAuthStateEntity state = new OAuthStateEntity();
        state.setState("valid-state");
        state.setUserId(USER_ID);
        state.setExpiresAt(Instant.now().plusSeconds(300));

        when(oAuthStateRepository.findById("valid-state")).thenReturn(Optional.of(state));
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "access_token",  "access-tok",
                        "refresh_token", "refresh-tok",
                        "expires_in",    3600)));
        when(encryptor.encrypt(anyString())).thenReturn("encrypted");
        when(tokenRepository.findById(USER_ID)).thenReturn(Optional.empty());

        service.exchangeCodeForToken("auth-code", "valid-state");

        verify(oAuthStateRepository).delete(state);
        verify(tokenRepository).save(any(SpotifyTokenEntity.class));
    }

    @Test
    void exchangeCodeForToken_unknownState_throwsIllegalArgumentException() {
        when(oAuthStateRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exchangeCodeForToken("code", "unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void exchangeCodeForToken_expiredState_deletesStateAndThrows() {
        OAuthStateEntity expired = new OAuthStateEntity();
        expired.setState("expired-state");
        expired.setUserId(USER_ID);
        expired.setExpiresAt(Instant.now().minusSeconds(10));

        when(oAuthStateRepository.findById("expired-state")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.exchangeCodeForToken("code", "expired-state"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirado");

        verify(oAuthStateRepository).delete(expired);
    }

    // ── getTokenForUser ──────────────────────────────────────────────────────

    @Test
    void getTokenForUser_validToken_returnsDecryptedToken() {
        SpotifyTokenEntity token = new SpotifyTokenEntity();
        token.setAccessToken("encrypted-access");
        token.setExpiresAt(Instant.now().plusSeconds(3600)); // plenty of margin

        when(tokenRepository.findById(USER_ID)).thenReturn(Optional.of(token));
        when(encryptor.decrypt("encrypted-access")).thenReturn("plain-access-token");

        String result = service.getTokenForUser(USER_ID);

        assertThat(result).isEqualTo("plain-access-token");
        verifyNoInteractions(restTemplate); // no refresh triggered
    }

    @Test
    void getTokenForUser_nearExpiry_refreshesTokenAndReturnsNewOne() {
        SpotifyTokenEntity token = new SpotifyTokenEntity();
        token.setUserId(USER_ID);
        token.setAccessToken("old-encrypted-access");
        token.setRefreshToken("old-encrypted-refresh");
        token.setExpiresAt(Instant.now().plusSeconds(60)); // within 5-min refresh margin

        when(tokenRepository.findById(USER_ID)).thenReturn(Optional.of(token));
        // refreshToken() decrypts old refresh to send to Spotify
        when(encryptor.decrypt("old-encrypted-refresh")).thenReturn("plain-refresh");
        // Spotify returns a new access token (no new refresh token in response)
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "access_token", "new-access",
                        "expires_in",   3600)));
        // encrypting new tokens to persist
        when(encryptor.encrypt("new-access")).thenReturn("new-encrypted-access");
        when(encryptor.encrypt("plain-refresh")).thenReturn("re-encrypted-refresh");
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // final decrypt of the new access token to return to caller
        when(encryptor.decrypt("new-encrypted-access")).thenReturn("new-access");

        String result = service.getTokenForUser(USER_ID);

        verify(restTemplate).postForEntity(anyString(), any(), eq(Map.class));
        assertThat(result).isEqualTo("new-access");
    }

    @Test
    void getTokenForUser_notLinked_throwsIllegalArgumentException() {
        when(tokenRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTokenForUser(USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vinculado");
    }
}