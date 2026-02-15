package es.wara.ecos.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración de RestTemplate para peticiones HTTP a APIs externas
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Bean de RestTemplate configurado con timeouts apropiados
     * para evitar que las peticiones a APIs externas bloqueen la aplicación
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        return restTemplate;
    }

    /**
     * Configuración de la fábrica de peticiones HTTP con timeouts
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // Timeout de conexión: 5 segundos
        factory.setReadTimeout(10000);    // Timeout de lectura: 10 segundos
        return factory;
    }
}