package es.wara.ecos.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// Cargar variables de entorno desde .env antes de iniciar Spring
		try {
			Dotenv dotenv = Dotenv.configure()
					.ignoreIfMissing() // No falla si el .env no existe (útil en producción)
					.load();

			// Establecer las variables como propiedades del sistema
			dotenv.entries().forEach(entry -> {
				System.setProperty(entry.getKey(), entry.getValue());
			});

			System.out.println("✓ Variables de entorno cargadas desde .env");
		} catch (Exception e) {
			System.err.println("⚠ No se pudo cargar el archivo .env: " + e.getMessage());
		}

		SpringApplication.run(BackendApplication.class, args);
	}

}
