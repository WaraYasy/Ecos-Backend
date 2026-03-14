package es.wara.ecos.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;

/**
 * Configuración central del cliente HTTP para peticiones a APIs externas.
 *
 * <p>Proporciona un {@link RestTemplate} configurado con:</p>
 * <ul>
 *   <li><b>Timeouts</b>: 5 s de conexión, 10 s de lectura.</li>
 *   <li><b>Logging</b>: registra método HTTP, URI y código de respuesta de cada petición.</li>
 *   <li><b>Retry</b>: habilitado globalmente vía {@code @EnableRetry}; los clientes
 *       anotan sus métodos con {@code @Retryable} para reintentos automáticos con
 *       backoff exponencial.</li>
 *   <li><b>Proxy</b>: soporte opcional de proxy HTTP configurable por perfil de entorno.
 *       Se activa cuando {@code http.proxy.enabled=true} en las propiedades. Admite
 *       proxies autenticados (usuario/contraseña).</li>
 * </ul>
 *
 * <h2>Uso del proxy por entorno</h2>
 * <pre>
 * dev  → deshabilitado por defecto; activar para inspección con mitmproxy/Charles
 * prod → configurado vía variables de entorno PROXY_HOST, PROXY_PORT, etc.
 * test → siempre deshabilitado; los tests usan MockRestServiceServer o WireMock
 * </pre>
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 * @see ProxyProperties
 */
@Slf4j
@EnableRetry
@Configuration
@EnableConfigurationProperties(ProxyProperties.class)
public class RestTemplateConfig {

    /** Propiedades de proxy leídas del fichero de configuración del perfil activo. */
    private final ProxyProperties proxyProperties;

    /**
     * Constructor con inyección de las propiedades de proxy.
     *
     * @param proxyProperties propiedades {@code http.proxy.*} del perfil activo
     */
    public RestTemplateConfig(ProxyProperties proxyProperties) {
        this.proxyProperties = proxyProperties;
    }

    /**
     * Bean de RestTemplate con timeouts, interceptor de logging, soporte de retry y proxy opcional.
     *
     * <p>Se usa {@link BufferingClientHttpRequestFactory} para poder leer el cuerpo
     * de la respuesta tanto en el interceptor de logging como en el llamador real.
     * Sin este wrapper, leer el body en el interceptor lo consumiría y el llamador
     * recibiría un stream vacío.</p>
     *
     * @return RestTemplate listo para usar en los clientes HTTP externos
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(
                new BufferingClientHttpRequestFactory(clientHttpRequestFactory())
        );
        restTemplate.setInterceptors(List.of(loggingInterceptor()));
        return restTemplate;
    }

    /**
     * Crea la fábrica de peticiones HTTP con timeouts y, opcionalmente, proxy.
     *
     * <p>Si {@code http.proxy.enabled=true}, delega en {@link #configureProxy}
     * para añadir el proxy al factory antes de devolverlo.</p>
     *
     * @return {@link SimpleClientHttpRequestFactory} configurado
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Timeout de conexión: tiempo máximo para establecer el TCP handshake
        factory.setConnectTimeout(5000);

        // Timeout de lectura: tiempo máximo esperando la respuesta completa
        factory.setReadTimeout(10000);

        // Configurar proxy solo si está habilitado en las propiedades del perfil activo
        if (proxyProperties.isEnabled()) {
            configureProxy(factory);
        }

        return factory;
    }

    /**
     * Configura el proxy HTTP en el factory proporcionado.
     *
     * <p>Si el proxy tiene credenciales definidas ({@code http.proxy.user}),
     * registra un {@link Authenticator} global para que Java las envíe
     * automáticamente en la cabecera {@code Proxy-Authorization} cuando
     * el servidor proxy lo solicite.</p>
     *
     * <p><b>Nota sobre {@code Authenticator.setDefault}:</b> es una configuración
     * global de la JVM. En tests esto no aplica porque el proxy está deshabilitado
     * ({@code http.proxy.enabled=false} en {@code application-test.properties}).</p>
     *
     * @param factory fábrica de peticiones donde se registra el proxy
     */
    private void configureProxy(SimpleClientHttpRequestFactory factory) {
        // Crear la dirección del proxy (host + puerto)
        InetSocketAddress proxyAddress = new InetSocketAddress(
                proxyProperties.getHost(),
                proxyProperties.getPort()
        );

        // Registrar el proxy de tipo HTTP en el factory
        factory.setProxy(new Proxy(Proxy.Type.HTTP, proxyAddress));

        log.info("Proxy HTTP habilitado → {}:{}", proxyProperties.getHost(), proxyProperties.getPort());

        // Si el proxy requiere autenticación, registrar las credenciales
        if (proxyProperties.hasCredentials()) {
            configureProxyAuthentication();
        }
    }

    /**
     * Registra un {@link Authenticator} global para proxies autenticados.
     *
     * <p>Java lo invoca automáticamente cuando el proxy devuelve un
     * {@code 407 Proxy Authentication Required}. El authenticator responde
     * con las credenciales configuradas en {@code http.proxy.user} y
     * {@code http.proxy.password}.</p>
     *
     * <p>La contraseña se maneja como {@code char[]} (en lugar de {@link String})
     * para que pueda limpiarse de memoria tras su uso, siguiendo las
     * recomendaciones de seguridad de Java.</p>
     */
    private void configureProxyAuthentication() {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // Responder solo a peticiones de autenticación del PROXY,
                // no a autenticaciones del servidor de destino (tipo SERVER)
                if (getRequestorType() == RequestorType.PROXY) {
                    return new PasswordAuthentication(
                            proxyProperties.getUser(),
                            proxyProperties.getPassword().toCharArray()
                    );
                }
                return null;
            }
        });

        log.info("Autenticación de proxy configurada para usuario: {}", proxyProperties.getUser());
    }

    /**
     * Interceptor que registra el método HTTP, la URI y el código de respuesta
     * de cada petición saliente a APIs externas.
     *
     * <p>El nivel {@code DEBUG} permite activarlo/desactivarlo sin cambiar código,
     * mediante {@code logging.level.es.wara.ecos=DEBUG} en las propiedades.</p>
     *
     * @return interceptor de logging para el {@link RestTemplate}
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("→ HTTP {} {}", request.getMethod(), request.getURI());

            var response = execution.execute(request, body);

            log.debug("← HTTP {} {} → {}",
                    request.getMethod(),
                    request.getURI(),
                    response.getStatusCode().value());

            return response;
        };
    }
}