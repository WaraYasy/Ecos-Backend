package es.wara.ecos.backend.infrastructure.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Propiedades de configuración del seeder de tendencias.
 *
 * <p>Se cargan desde {@code seed-terms.yml} (incluido en el JAR como valores por defecto)
 * y pueden sobreescribirse con un fichero externo montado en {@code /config/seed-terms.yml}
 * (configurado con {@code spring.config.import=optional:file:/config/seed-terms.yml}).</p>
 *
 * <p>Para añadir nuevos géneros, estéticas o películas basta con editar el fichero
 * externo y reiniciar el servicio — sin recompilar ni redesplegar el JAR.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "seed")
public class SeedTermsProperties {

    /** Activa el seed inicial al arrancar (si la BD tiene menos de {@code minSongs}). */
    private boolean enabled = false;

    /** Expresión cron del refresco periódico de tendencias. */
    private String cron = "0 0 3 * * SUN";

    /** Canciones a buscar por término. */
    private int songsPerTerm = 20;

    /** Milisegundos de espera entre términos para no saturar las APIs. */
    private long delayMs = 3000;

    /** Mínimo de canciones en BD para saltar el seed inicial. */
    private int minSongs = 200;

    private List<String> genres = new ArrayList<>();
    private List<String> aesthetics = new ArrayList<>();
    private List<String> movies = new ArrayList<>();
    private List<String> decades = new ArrayList<>();
    private List<String> countries = new ArrayList<>();

    // ── Getters y setters ────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }

    public int getSongsPerTerm() { return songsPerTerm; }
    public void setSongsPerTerm(int songsPerTerm) { this.songsPerTerm = songsPerTerm; }

    public long getDelayMs() { return delayMs; }
    public void setDelayMs(long delayMs) { this.delayMs = delayMs; }

    public int getMinSongs() { return minSongs; }
    public void setMinSongs(int minSongs) { this.minSongs = minSongs; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public List<String> getAesthetics() { return aesthetics; }
    public void setAesthetics(List<String> aesthetics) { this.aesthetics = aesthetics; }

    public List<String> getMovies() { return movies; }
    public void setMovies(List<String> movies) { this.movies = movies; }

    public List<String> getDecades() { return decades; }
    public void setDecades(List<String> decades) { this.decades = decades; }

    public List<String> getCountries() { return countries; }
    public void setCountries(List<String> countries) { this.countries = countries; }
}
