package es.wara.ecos.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Configuración del sistema de caché in-memory de ECOS.
 *
 * <p>Implementa la <b>Capa 2</b> de la estrategia de caché del proyecto (ver {@code docs/CACHE_STRATEGY.md}).
 * Usa <b>Caffeine</b> como implementación in-memory con TTL diferenciado por tipo de dato,
 * siguiendo las políticas definidas en el plan de desarrollo (tarea 2.13).</p>
 *
 * <h2>Caches disponibles</h2>
 * <table border="1">
 *   <tr><th>Nombre</th><th>TTL</th><th>Fuente</th><th>Justificación</th></tr>
 *   <tr><td>{@code audio-features}</td><td>7 días</td><td>Spotify</td><td>Dato estático, nunca cambia</td></tr>
 *   <tr><td>{@code song-tags}</td><td>6 horas</td><td>Last.fm</td><td>Actualizado por la comunidad</td></tr>
 *   <tr><td>{@code movies-for-song}</td><td>24 horas</td><td>MusicBrainz + TMDb</td><td>Metadatos estables</td></tr>
 *   <tr><td>{@code soundtrack-songs}</td><td>24 horas</td><td>MusicBrainz + Spotify</td><td>Metadatos estables</td></tr>
 *   <tr><td>{@code search-songs}</td><td>15 minutos</td><td>Spotify</td><td>Pueden aparecer novedades</td></tr>
 * </table>
 *
 * <p>Cada cache tiene un máximo de 500 entradas y registra estadísticas de hits/misses
 * disponibles en {@code /actuator/metrics/cache.*}.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 * @see <a href="../../../../../../../../docs/CACHE_STRATEGY.md">Estrategia de caché de dos capas</a>
 */
@EnableCaching
@Configuration
public class CacheConfig {

    /** Nombre del cache para audio features de Spotify. TTL: 7 días. */
    public static final String CACHE_AUDIO_FEATURES   = "audio-features";

    /** Nombre del cache para tags de Last.fm. TTL: 6 horas. */
    public static final String CACHE_SONG_TAGS        = "song-tags";

    /** Nombre del cache para películas asociadas a una canción (MusicBrainz + TMDb). TTL: 24 horas. */
    public static final String CACHE_MOVIES_FOR_SONG  = "movies-for-song";

    /** Nombre del cache para canciones de un OST (MusicBrainz + Spotify). TTL: 24 horas. */
    public static final String CACHE_SOUNDTRACK_SONGS = "soundtrack-songs";

    /** Nombre del cache para resultados de búsqueda de canciones (Spotify). TTL: 15 minutos. */
    public static final String CACHE_SEARCH_SONGS     = "search-songs";

    /**
     * Configura el {@link CacheManager} con todos los caches de la aplicación.
     *
     * <p>Cada cache se construye individualmente con su propio TTL usando
     * {@link #buildCache(String, Duration)}. Se usa {@link SimpleCacheManager}
     * para registrar explícitamente la lista de caches.</p>
     *
     * @return gestor de caches con política TTL diferenciada por tipo de dato
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache(CACHE_AUDIO_FEATURES,   Duration.ofDays(7)),
                buildCache(CACHE_SONG_TAGS,        Duration.ofHours(6)),
                buildCache(CACHE_MOVIES_FOR_SONG,  Duration.ofHours(24)),
                buildCache(CACHE_SOUNDTRACK_SONGS, Duration.ofHours(24)),
                buildCache(CACHE_SEARCH_SONGS,     Duration.ofMinutes(15))
        ));
        return manager;
    }

    /**
     * Construye un {@link CaffeineCache} con el nombre y TTL indicados.
     *
     * <p>Configuración fija para todos los caches:
     * <ul>
     *   <li>Máximo 500 entradas (evita consumo excesivo de memoria)</li>
     *   <li>Expiración tras la escritura (no tras el acceso)</li>
     *   <li>{@code recordStats()} activo — métricas en {@code /actuator/metrics/cache.*}</li>
     * </ul>
     * </p>
     *
     * @param name nombre del cache (usar las constantes de esta clase)
     * @param ttl  tiempo de vida de las entradas tras su escritura
     * @return instancia de {@link CaffeineCache} lista para registrar en el {@link CacheManager}
     */
    private CaffeineCache buildCache(String name, Duration ttl) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttl)
                        .maximumSize(500)
                        .recordStats()
                        .build()
        );
    }
}