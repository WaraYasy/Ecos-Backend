package es.wara.ecos.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración del proxy HTTP para peticiones a APIs externas.
 *
 * <p>Se leen del fichero de propiedades bajo el prefijo {@code http.proxy} y
 * permiten habilitar un proxy por perfil de entorno (dev, prod, test):</p>
 *
 * <ul>
 *   <li><b>dev</b>: proxy deshabilitado por defecto; habilitar para inspeccionar
 *       tráfico con herramientas como mitmproxy o Charles Proxy.</li>
 *   <li><b>prod</b>: proxy corporativo configurado vía variables de entorno
 *       ({@code PROXY_HOST}, {@code PROXY_PORT}, etc.).</li>
 *   <li><b>test</b>: proxy siempre deshabilitado; los tests usan
 *       {@code MockRestServiceServer} o WireMock para interceptar las llamadas.</li>
 * </ul>
 *
 * <p>Ejemplo en {@code application-dev.properties}:</p>
 * <pre>
 * http.proxy.enabled=true
 * http.proxy.host=localhost
 * http.proxy.port=8888
 * </pre>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 2.8.0
 * @see RestTemplateConfig
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "http.proxy")
public class ProxyProperties {

    /**
     * Indica si el proxy HTTP está habilitado.
     * <p>Valor por defecto: {@code false} — las peticiones van directamente a las APIs.</p>
     */
    private boolean enabled = false;

    /**
     * Nombre de host o dirección IP del servidor proxy.
     * <p>Obligatorio cuando {@code enabled = true}. Ejemplo: {@code localhost},
     * {@code proxy.miempresa.com}.</p>
     */
    private String host;

    /**
     * Puerto del servidor proxy.
     * <p>Valor por defecto: {@code 8080}. mitmproxy usa 8080 por defecto;
     * Charles Proxy usa 8888.</p>
     */
    private int port = 8080;

    /**
     * Nombre de usuario para autenticación en el proxy.
     * <p>Opcional. Solo necesario si el proxy requiere credenciales
     * (habitual en proxies corporativos).</p>
     */
    private String user;

    /**
     * Contraseña para autenticación en el proxy.
     * <p>Opcional. Se usa junto con {@link #user} cuando el proxy es autenticado.</p>
     */
    private String password;

    /**
     * Indica si el proxy está configurado con credenciales de autenticación.
     *
     * @return {@code true} si {@code user} tiene valor y no está en blanco
     */
    public boolean hasCredentials() {
        return user != null && !user.isBlank();
    }
}