package es.wara.ecos.backend.infrastructure.exception;

/**
 * Excepción específica para errores del servidor de APIs externas.
 * <p>
 * Esta excepción se lanza cuando:
 * <ul>
 *   <li>El servidor de la API responde con códigos HTTP 5xx</li>
 *   <li>Hay problemas internos en el servidor de la API (500 Internal Server Error)</li>
 *   <li>El servicio de la API no está disponible (503 Service Unavailable)</li>
 *   <li>Ocurre un timeout en el gateway de la API (504 Gateway Timeout)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Códigos HTTP comunes:</b>
 * <ul>
 *   <li><b>500</b> - Internal Server Error: Error interno del servidor</li>
 *   <li><b>502</b> - Bad Gateway: Gateway inválido</li>
 *   <li><b>503</b> - Service Unavailable: Servicio no disponible (mantenimiento o sobrecarga)</li>
 *   <li><b>504</b> - Gateway Timeout: Timeout en el gateway</li>
 * </ul>
 * </p>
 * <p>
 * <b>Acciones recomendadas:</b>
 * <ul>
 *   <li>Implementar lógica de retry con backoff exponencial</li>
 *   <li>Usar circuit breaker para evitar peticiones repetidas a servicios caídos</li>
 *   <li>Verificar el estado de la API en su página de status</li>
 *   <li>Considerar fallbacks o servicios alternativos</li>
 *   <li>Monitorear y alertar sobre errores recurrentes</li>
 * </ul>
 * </p>
 * <p>
 * Estos errores suelen ser temporales y están fuera del control de la aplicación.
 * </p>
 *
 * @see ExternalApiException
 * @since 1.0.0
 */
public class ApiServerException extends ExternalApiException {

    /**
     * Crea una nueva excepción de error del servidor.
     *
     * @param apiName Nombre de la API que generó el error
     * @param statusCode Código HTTP 5xx (ej: 500, 502, 503, 504)
     * @param message Mensaje descriptivo del error
     */
    public ApiServerException(String apiName, Integer statusCode, String message) {
        super(apiName, statusCode, message);
    }

    /**
     * Crea una nueva excepción de error del servidor con causa raíz.
     * <p>
     * Útil cuando el error del servidor es causado por otro problema
     * (ej: timeout de red, error de conexión).
     * </p>
     *
     * @param apiName Nombre de la API que generó el error
     * @param statusCode Código HTTP 5xx
     * @param message Mensaje descriptivo del error
     * @param cause Excepción que causó este error
     */
    public ApiServerException(String apiName, Integer statusCode, String message, Throwable cause) {
        super(apiName, statusCode, message, cause);
    }
}