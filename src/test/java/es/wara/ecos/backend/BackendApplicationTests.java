package es.wara.ecos.backend;

import es.wara.ecos.backend.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifica que el contexto de Spring arranca correctamente
 * con PostgreSQL real (Testcontainers) y todas las migraciones Flyway aplicadas.
 */
class BackendApplicationTests extends BaseIntegrationTest {

	@Test
	void contextLoads() {
	}

}
