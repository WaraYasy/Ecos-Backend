package es.wara.ecos.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.Executor;

/**
 * Reemplaza el exportExecutor con un executor síncrono en tests.
 * Evita que tareas @Async (como CalculateUserProfileUseCase.executeAsync)
 * corran en background y causen deadlocks con los TRUNCATE de @BeforeEach.
 */
@TestConfiguration
public class TestAsyncConfig {

    @Profile("test")
    @Bean(name = "exportExecutor")
    public Executor exportExecutor() {
        return Runnable::run;
    }
}