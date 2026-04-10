package es.wara.ecos.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración del microservicio de IA (FastAPI Python).
 *
 * <p>Se leen del fichero de propiedades bajo el prefijo {@code ai.service}:</p>
 * <pre>
 * ai.service.url=https://mi-micro-ia.railway.app
 * ai.service.api-key=${AI_SERVICE_API_KEY}
 * </pre>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceProperties {

    /**
     * URL base del microservicio Python (sin barra final).
     * Ejemplo: {@code https://ecos-ai.railway.app}
     */
    private String url;

    /**
     * Clave secreta compartida para el header {@code X-API-Key}.
     * Debe coincidir con la variable {@code API_KEY} configurada en el micro Python.
     */
    private String apiKey;

    /**
     * Indica si el micro de IA está disponible para llamadas.
     * Si {@code url} está vacío o en blanco, las llamadas se omiten sin error.
     */
    public boolean isAvailable() {
        return url != null && !url.isBlank();
    }

    public String getUrl() {
        return url != null ? url.strip() : null;
    }
}