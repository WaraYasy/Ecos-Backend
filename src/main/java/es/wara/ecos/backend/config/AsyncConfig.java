package es.wara.ecos.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración del pool de hilos para tareas asíncronas ({@code @Async}).
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Pool compartido para exportaciones a Spotify y recálculo de perfiles de usuario.
     * Core=2 hilos siempre activos, máximo 5, cola de 100 tareas antes de rechazar.
     */
    @Profile("!test")
    @Bean(name = "exportExecutor")
    public Executor exportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("export-");
        executor.initialize();
        return executor;
    }
}