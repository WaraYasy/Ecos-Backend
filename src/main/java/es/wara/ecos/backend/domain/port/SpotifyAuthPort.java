package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.interfaces.rest.dto.LoginResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de dominio para el flujo de autorización de Spotify (Authorization Code Flow).
 * <p>
 * Desacopla la capa de aplicación de la implementación concreta de autenticación con Spotify.
 * </p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
public interface SpotifyAuthPort {

    /**
     * Genera la URL de autorización de Spotify para vincular cuenta (requiere userId existente).
     *
     * @param userId identificador del usuario en ECOS
     * @return URL completa de autorización de Spotify
     */
    String getAuthorizationUrl(UUID userId);

    /**
     * Genera la URL de autorización de Spotify para login (sin userId previo).
     *
     * @return URL completa de autorización de Spotify
     */
    String getLoginUrl();

    /**
     * Intercambia el código de autorización por tokens y los persiste.
     * <p>
     * Si el state corresponde a un flujo LOGIN, busca o crea el usuario en ECOS
     * y devuelve el JWT. Si es LINK, vincula la cuenta y devuelve vacío.
     * </p>
     *
     * @param code  código devuelto por Spotify en el callback
     * @param state token anti-CSRF generado al iniciar el flujo
     * @return JWT + userId si es flujo LOGIN; vacío si es flujo LINK
     */
    Optional<LoginResponse> exchangeCodeForToken(String code, String state);

    /**
     * Devuelve el access token vigente del usuario, refrescándolo si está próximo a expirar.
     *
     * @param userId identificador del usuario
     * @return access token de Spotify listo para usar
     * @throws IllegalArgumentException si el usuario no ha vinculado su cuenta
     */
    String getTokenForUser(UUID userId);

    /**
     * Desvincula la cuenta de Spotify del usuario eliminando su token almacenado.
     * <p>
     * Se bloquea con {@code ConflictException} si el usuario no tiene contraseña propia,
     * ya que quedaría sin forma de autenticarse en ECOS.
     * </p>
     *
     * @param userId identificador del usuario
     * @throws ConflictException        si el usuario no tiene contraseña y dependería de Spotify para entrar
     * @throws IllegalArgumentException si el usuario no ha vinculado su cuenta de Spotify
     */
    void unlinkSpotify(UUID userId);
}
