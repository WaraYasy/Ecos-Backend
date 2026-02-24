package es.wara.ecos.backend.infrastructure.exception;

/**
 * Excepción específica para errores de autenticación con APIs externas.
 * <p>
 * Esta excepción se lanza cuando:
 * <ul>
 *   <li>Las credenciales proporcionadas (API keys, tokens, etc.) son inválidas</li>
 *   <li>Las credenciales no tienen los permisos necesarios para realizar la operación</li>
 *   <li>El servidor responde con códigos HTTP 401 (Unauthorized) o 403 (Forbidden)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Acciones recomendadas:</b>
 * <ul>
 *   <li>Verificar que las credenciales en el archivo .env sean correctas</li>
 *   <li>Confirmar que las credenciales tienen los permisos necesarios en la API</li>
 *   <li>Revisar que las credenciales no hayan expirado o sido revocadas</li>
 * </ul>
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @see ExternalApiException
 * @since 1.0.0
 */
public class ApiAuthenticationException extends ExternalApiException {

    /**
     * Crea una nueva excepción de autenticación con código HTTP 401 por defecto.
     *
     * @param apiName Nombre de la API que generó el error
     * @param message Mensaje descriptivo del error de autenticación
     */
    public ApiAuthenticationException(String apiName, String message) {
        super(apiName, 401, message);
    }

    /**
     * Crea una nueva excepción de autenticación con código HTTP personalizado.
     * Útil para diferenciar entre 401 (Unauthorized) y 403 (Forbidden).
     *
     * @param apiName Nombre de la API que generó el error
     * @param statusCode Código HTTP (normalmente 401 o 403)
     * @param message Mensaje descriptivo del error de autenticación
     */
    public ApiAuthenticationException(String apiName, Integer statusCode, String message) {
        super(apiName, statusCode, message);
    }
}