package es.wara.ecos.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Clase base para tests de integración y E2E.
 *
 * Levanta un contenedor PostgreSQL real (pgvector/pgvector:pg16) una sola vez
 * por sesión de tests y lo comparte entre todas las clases que extiendan esta.
 *
 * @ServiceConnection le indica a Spring Boot que conecte automáticamente
 * al contenedor sin necesidad de configurar manualmente la URL de la BD.
 * Flyway ejecuta todas las migraciones al arrancar, igual que en producción.
 *
 * Uso:
 *   class MiRepositorioTest extends BaseIntegrationTest { ... }
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("ecos_test")
                    .withUsername("ecos")
                    .withPassword("ecos_test");
}