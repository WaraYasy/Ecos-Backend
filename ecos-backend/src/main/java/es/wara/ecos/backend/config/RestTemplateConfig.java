package es.wara.ecos.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración de {@link RestTemplate} para peticiones HTTP a APIs externas.
 * <p>
 * Esta clase proporciona un bean de RestTemplate configurado con timeouts
 * apropiados para evitar que las peticiones a servicios externos bloqueen
 * indefinidamente la aplicación.
 * </p>
 * <p>
 * <b>Timeouts configurados:</b>
 * <ul>
 *   <li><b>Connection Timeout</b>: 5 segundos - Tiempo máximo para establecer conexión</li>
 *   <li><b>Read Timeout</b>: 10 segundos - Tiempo máximo para leer la respuesta</li>
 * </ul>
 * </p>
 * <p>
 * Este RestTemplate es utilizado por todos los clientes de APIs externas
 * (SpotifyClient, LastFmClient, TmdbClient) para garantizar comportamiento
 * consistente y evitar timeouts indefinidos.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @see org.springframework.web.client.RestTemplate
 * @since 1.0.0
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Crea y configura el bean de RestTemplate.
     * <p>
     * El RestTemplate creado incluye configuración de timeouts para
     * proteger la aplicación de bloqueos por servicios externos lentos
     * o no disponibles.
     * </p>
     *
     * @return RestTemplate configurado con timeouts de 5s (conexión) y 10s (lectura)
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        return restTemplate;
    }

    /**
     * Crea la fábrica de peticiones HTTP con configuración de timeouts.
     * <p>
     * Utiliza {@link SimpleClientHttpRequestFactory} que es la implementación
     * por defecto de Spring para peticiones HTTP síncronas.
     * </p>
     *
     * @return ClientHttpRequestFactory configurado con timeouts
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // Timeout de conexión: 5 segundos
        factory.setReadTimeout(10000);    // Timeout de lectura: 10 segundos
        return factory;
    }
}