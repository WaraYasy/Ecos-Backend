package es.wara.ecos.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Configuración central del cliente HTTP para peticiones a APIs externas.
 * <p>
 * Proporciona un {@link RestTemplate} configurado con:
 * <ul>
 *   <li><b>Timeouts</b>: 5s de conexión, 10s de lectura</li>
 *   <li><b>Logging</b>: registra cada petición y su código de respuesta HTTP</li>
 *   <li><b>Retry</b>: habilitado globalmente vía {@code @EnableRetry}; los clientes
 *       anotan sus métodos con {@code @Retryable} para reintentos automáticos</li>
 * </ul>
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@EnableRetry
@Configuration
public class RestTemplateConfig {

    /**
     * Bean de RestTemplate con timeouts, interceptor de logging y soporte de retry.
     * <p>
     * Se usa {@link BufferingClientHttpRequestFactory} para poder leer el cuerpo
     * de la respuesta tanto en el interceptor de logging como en el llamador real.
     * </p>
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
     * Crea la fábrica de peticiones HTTP con timeouts configurados.
     *
     * @return {@link SimpleClientHttpRequestFactory} con 5s conexión y 10s lectura
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // Timeout de conexión: 5 segundos
        factory.setReadTimeout(10000);    // Timeout de lectura: 10 segundos
        return factory;
    }

    /**
     * Interceptor que registra el método HTTP, la URI y el código de respuesta
     * de cada petición saliente a APIs externas.
     *
     * @return interceptor de logging para RestTemplate
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("→ HTTP {} {}", request.getMethod(), request.getURI());
            var response = execution.execute(request, body);
            log.debug("← HTTP {} {} → {}", request.getMethod(), request.getURI(), response.getStatusCode().value());
            return response;
        };
    }
}