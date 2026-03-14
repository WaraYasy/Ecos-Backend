package es.wara.ecos.backend.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Clase base para tests funcionales HTTP.
 *
 * El contenedor PostgreSQL se arranca una sola vez al cargar la clase (bloque static)
 * y vive durante toda la sesión JVM. Esto evita que Testcontainers lo detenga entre
 * clases de test, lo que provocaría CannotGetJdbcConnection en la segunda clase.
 *
 * @DynamicPropertySource registra las propiedades de conexión antes de que Spring
 * cree el contexto. Todas las clases que extiendan BaseFunctionalTest compartirán
 * el mismo contexto Spring gracias al caché de contextos de Spring Test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseFunctionalTest {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("ecos_test")
                    .withUsername("ecos")
                    .withPassword("ecos_test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
