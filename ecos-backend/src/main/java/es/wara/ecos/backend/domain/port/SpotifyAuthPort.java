package es.wara.ecos.backend.domain.port;

/**
 * Puerto de dominio para el flujo de autorización de Spotify (Authorization Code Flow).
 * <p>
 * Desacopla la capa de aplicación de la implementación concreta de autenticación con Spotify.
 * </p>
 *
 * @author Wara Pacheco
 */
public interface SpotifyAuthPort {

    /**
     * Genera la URL de autorización de Spotify para que el usuario vincule su cuenta.
     *
     * @param userId identificador del usuario en ECOS
     * @return URL completa de autorización de Spotify
     */
    String getAuthorizationUrl(Long userId);

    /**
     * Intercambia el código de autorización por tokens y los persiste.
     *
     * @param code  código devuelto por Spotify en el callback
     * @param state userId de ECOS enviado como state en el flujo OAuth
     */
    void exchangeCodeForToken(String code, String state);

    /**
     * Devuelve el access token vigente del usuario, refrescándolo si está próximo a expirar.
     *
     * @param userId identificador del usuario
     * @return access token de Spotify listo para usar
     * @throws IllegalArgumentException si el usuario no ha vinculado su cuenta
     */
    String getTokenForUser(Long userId);
}
