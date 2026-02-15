package es.wara.ecos.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación Ecos Backend.
 * <p>
 * Esta aplicación proporciona un backend para la gestión de recomendaciones
 * de contenido multimedia (música, películas, series) integrando múltiples
 * API externas como Spotify, Last.fm y TMDB.
 * </p>
 * <p>
 * <b>Características principales:</b>
 * <ul>
 *   <li>Integración con Spotify API para datos musicales</li>
 *   <li>Integración con Last.fm para información de artistas y canciones</li>
 *   <li>Integración con TMDB para películas y series</li>
 *   <li>Carga automática de variables de entorno desde archivo .env</li>
 *   <li>Sistema robusto de manejo de errores de APIs</li>
 *   <li>Logging completo con rotación de archivos</li>
 * </ul>
 * </p>
 * <p>
 * La aplicación carga automáticamente las credenciales de las API desde un
 * archivo {@code .env} en el directorio raíz del proyecto antes de iniciar
 * el contexto de Spring Boot.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@SpringBootApplication
public class BackendApplication {

	/**
	 * Punto de entrada de la aplicación.
	 * <p>
	 * Esta función realiza las siguientes operaciones:
	 * <ol>
	 *   <li>Carga las variables de entorno desde el archivo .env</li>
	 *   <li>Establece las variables como propiedades del sistema</li>
	 *   <li>Inicia el contexto de Spring Boot</li>
	 * </ol>
	 * </p>
	 * <p>
	 * Si el archivo .env no existe, la aplicación continúa su ejecución
	 * (útil en entornos de producción donde las variables se configuran
	 * directamente en el sistema operativo).
	 * </p>
	 *
	 * @param args Argumentos de línea de comandos (no utilizados actualmente)
	 */
	public static void main(String[] args) {
		// Cargar variables de entorno desde .env antes de iniciar Spring
		try {
			Dotenv dotenv = Dotenv.configure()
					.ignoreIfMissing() // No falla si el env no existe (útil en producción)
					.load();

			// Establecer las variables como propiedades del sistema
			dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

			System.out.println("✓ Variables de entorno cargadas desde .env");
		} catch (Exception e) {
			System.err.println("⚠ No se pudo cargar el archivo .env: " + e.getMessage());
		}

		SpringApplication.run(BackendApplication.class, args);
	}

}
