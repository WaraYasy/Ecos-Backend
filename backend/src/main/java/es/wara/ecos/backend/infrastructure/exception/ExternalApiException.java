package es.wara.ecos.backend.infrastructure.exception;

/**
 * Excepción base para errores al comunicarse con APIs externas.
 * <p>
 * Esta clase proporciona una jerarquía común de excepciones para todos los clientes
 * de APIs externas (Spotify, Last.fm, TMDB, etc.), facilitando el manejo uniforme
 * de errores en toda la aplicación.
 * </p>
 * <p>
 * La excepción incluye información contextual como el nombre de la API que generó
 * el error y opcionalmente el código de estado HTTP si el error proviene de una
 * respuesta HTTP.
 * </p>
 *
 * @see ApiAuthenticationException
 * @see ApiRateLimitException
 * @see ApiServerException
 * @since 1.0.0
 */
public class ExternalApiException extends RuntimeException {

    /**
     * Nombre de la API externa que generó la excepción.
     * Ejemplos: "Spotify", "Last.fm", "TMDB"
     */
    private final String apiName;

    /**
     * Código de estado HTTP asociado al error, si aplica.
     * Null si el error no está relacionado con una respuesta HTTP.
     */
    private final Integer statusCode;

    /**
     * Crea una nueva excepción de API externa sin código de estado HTTP.
     *
     * @param apiName Nombre de la API que generó el error (ej: "Spotify")
     * @param message Mensaje descriptivo del error
     */
    public ExternalApiException(String apiName, String message) {
        super(String.format("[%s API] %s", apiName, message));
        this.apiName = apiName;
        this.statusCode = null;
    }

    /**
     * Crea una nueva excepción de API externa con causa raíz.
     *
     * @param apiName Nombre de la API que generó el error
     * @param message Mensaje descriptivo del error
     * @param cause Excepción que causó este error
     */
    public ExternalApiException(String apiName, String message, Throwable cause) {
        super(String.format("[%s API] %s", apiName, message), cause);
        this.apiName = apiName;
        this.statusCode = null;
    }

    /**
     * Crea una nueva excepción de API externa con código de estado HTTP.
     *
     * @param apiName Nombre de la API que generó el error
     * @param statusCode Código de estado HTTP (ej: 404, 500)
     * @param message Mensaje descriptivo del error
     */
    public ExternalApiException(String apiName, Integer statusCode, String message) {
        super(String.format("[%s API] HTTP %d - %s", apiName, statusCode, message));
        this.apiName = apiName;
        this.statusCode = statusCode;
    }

    /**
     * Crea una nueva excepción de API externa con código de estado HTTP y causa raíz.
     *
     * @param apiName Nombre de la API que generó el error
     * @param statusCode Código de estado HTTP
     * @param message Mensaje descriptivo del error
     * @param cause Excepción que causó este error
     */
    public ExternalApiException(String apiName, Integer statusCode, String message, Throwable cause) {
        super(String.format("[%s API] HTTP %d - %s", apiName, statusCode, message), cause);
        this.apiName = apiName;
        this.statusCode = statusCode;
    }

    /**
     * Obtiene el nombre de la API que generó la excepción.
     *
     * @return Nombre de la API (ej: "Spotify", "Last.fm", "TMDB")
     */
    public String getApiName() {
        return apiName;
    }

    /**
     * Obtiene el código de estado HTTP asociado al error, si está disponible.
     *
     * @return Código de estado HTTP, o null si no aplica
     */
    public Integer getStatusCode() {
        return statusCode;
    }
}