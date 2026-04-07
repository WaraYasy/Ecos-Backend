package es.wara.ecos.backend;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada del backend de ECOS — app móvil de descubrimiento musical.
 *
 * <p><b>Arquitectura:</b> hexagonal (Ports &amp; Adapters) con Spring Boot 4 sobre Java 21.
 * Las capas son {@code interfaces/rest} → {@code application} → {@code domain} ←→ {@code infrastructure}.</p>
 *
 * <p><b>Integraciones externas:</b>
 * <ul>
 *   <li><b>Spotify Web API</b> — búsqueda de tracks, audio features, gestión de playlists</li>
 *   <li><b>Last.fm API</b> — tags de género/mood/estética, canciones similares</li>
 *   <li><b>MusicBrainz API</b> — puente bidireccional canción↔película (OST)</li>
 *   <li><b>TMDb API</b> — metadatos de películas (director, géneros, año)</li>
 * </ul>
 * </p>
 *
 * <p><b>Variables de entorno:</b> se cargan desde un archivo {@code .env} en la raíz
 * del proyecto mediante {@code dotenv-java} antes de arrancar el contexto de Spring.
 * En producción se inyectan directamente como variables del sistema (Railway, etc.).</p>
 *
 * <p>Puerto por defecto: {@code 3000}. Swagger UI disponible en {@code /swagger-ui.html}.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@EnableScheduling
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class BackendApplication {

	@Value("${server.port:8080}")
	private int port;

	@Value("${spring.profiles.active:default}")
	private String activeProfile;

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		log.info("========================================");
		log.info("  ECOS Backend arrancado correctamente");
		log.info("  Perfil activo : {}", activeProfile);
		log.info("  Puerto        : {}", port);
		log.info("  Swagger UI    : http://localhost:{}/swagger-ui.html", port);
		log.info("========================================");
	}

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

			System.out.println("[ECOS] Variables de entorno cargadas desde .env");
		} catch (Exception e) {
			System.err.println("[ECOS] No se pudo cargar .env (normal en prod): " + e.getMessage());
		}

		SpringApplication.run(BackendApplication.class, args);
	}

}
