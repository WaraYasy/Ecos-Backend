package es.wara.ecos.backend.infrastructure.adapter.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa la respuesta del endpoint de autenticación de Spotify.
 * <p>
 * Mapea la respuesta del flujo Client Credentials:
 * {@code POST https://accounts.spotify.com/api/token}
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyAuthTokenDTO {

    /** Token OAuth2 de acceso a la API de Spotify. */
    @JsonProperty("access_token")
    private String accessToken;

    /** Tipo de token, siempre "Bearer". */
    @JsonProperty("token_type")
    private String tokenType;

    /** Tiempo de vida del token en segundos (normalmente 3600). */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /** Permisos concedidos al token (en Client Credentials es null). */
    private String scope;
}