package es.wara.ecos.backend.infrastructure.exception;

/**
 * Excepción específica para errores de límite de tasa (Rate Limit) de APIs externas.
 * <p>
 * Esta excepción se lanza cuando:
 * <ul>
 *   <li>Se excede el número máximo de peticiones permitidas en un período de tiempo</li>
 *   <li>El servidor responde con código HTTP 429 (Too Many Requests)</li>
 *   <li>Se alcanza algún límite de cuota de la API (diario, mensual, etc.)</li>
 * </ul>
 * </p>
 * <p>
 * <b>Acciones recomendadas:</b>
 * <ul>
 *   <li>Implementar lógica de retry con backoff exponencial</li>
 *   <li>Reducir la frecuencia de peticiones a la API</li>
 *   <li>Implementar caché para evitar peticiones redundantes</li>
 *   <li>Considerar actualizar el plan de la API si es necesario</li>
 * </ul>
 * </p>
 * <p>
 * Esta excepción puede incluir información sobre cuánto tiempo esperar antes de
 * reintentar la petición (campo {@code retryAfterSeconds}).
 * </p>
 *
 * @see ExternalApiException
 * @since 1.0.0
 */
public class ApiRateLimitException extends ExternalApiException {

    /**
     * Tiempo recomendado (en segundos) antes de reintentar la petición.
     * Puede ser null si la API no proporciona esta información.
     */
    private final Long retryAfterSeconds;

    /**
     * Crea una nueva excepción de límite de tasa sin tiempo de reintento especificado.
     *
     * @param apiName Nombre de la API que generó el error
     * @param message Mensaje descriptivo del error
     */
    public ApiRateLimitException(String apiName, String message) {
        super(apiName, 429, message);
        this.retryAfterSeconds = null;
    }

    /**
     * Crea una nueva excepción de límite de tasa con tiempo de reintento recomendado.
     * <p>
     * El tiempo de reintento se agrega automáticamente al mensaje de error.
     * </p>
     *
     * @param apiName Nombre de la API que generó el error
     * @param message Mensaje descriptivo del error
     * @param retryAfterSeconds Segundos a esperar antes de reintentar, o null si no se especifica
     */
    public ApiRateLimitException(String apiName, String message, Long retryAfterSeconds) {
        super(apiName, 429, message + (retryAfterSeconds != null ?
            " (Reintentar después de " + retryAfterSeconds + " segundos)" : ""));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Obtiene el tiempo recomendado (en segundos) antes de reintentar la petición.
     * <p>
     * Este valor puede provenir del header HTTP "Retry-After" o de la
     * documentación específica de la API.
     * </p>
     *
     * @return Segundos a esperar antes de reintentar, o null si no está disponible
     */
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}