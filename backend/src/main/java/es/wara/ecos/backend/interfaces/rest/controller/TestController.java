package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.infrastructure.adapter.lastfm.LastFmClient;
import es.wara.ecos.backend.infrastructure.adapter.spotify.SpotifyClient;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.TmdbClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST de prueba para verificar la conectividad con API externas.
 * <p>
 * Este controlador proporciona endpoints de diagnóstico para verificar que
 * las integraciones con las API externas (Spotify, Last.fm, TMDB) están
 * configuradas correctamente y funcionando.
 * </p>
 * <p>
 * <b>Endpoints disponibles:</b>
 * <ul>
 *   <li><b>GET /api/test/spotify</b> - Prueba conexión con Spotify API</li>
 *   <li><b>GET /api/test/lastfm</b> - Prueba conexión con Last.fm API</li>
 *   <li><b>GET /api/test/tmdb</b> - Prueba conexión con TMDB API</li>
 *   <li><b>GET /api/test/all</b> - Prueba todas las API simultáneamente</li>
 * </ul>
 * </p>
 * <p>
 * <b>Nota:</b> Este controlador está diseñado para propósitos de desarrollo
 * y testing. En producción, considere deshabilitarlo o protegerlo con autenticación.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private SpotifyClient spotifyClient;

    @Autowired
    private LastFmClient lastFmClient;

    @Autowired
    private TmdbClient tmdbClient;

    /**
     * Prueba la conexión con Spotify API.
     * <p>
     * Obtiene un access token de Spotify para verificar que las credenciales
     * (SPOTIFY_CLIENT_ID y SPOTIFY_CLIENT_SECRET) están correctamente configuradas
     * y que la aplicación puede autenticarse exitosamente.
     * </p>
     *
     * @return Map con información del resultado:
     *         <ul>
     *           <li><b>status</b>: "success" o "error"</li>
     *           <li><b>api</b>: "Spotify"</li>
     *           <li><b>message</b>: Mensaje descriptivo del resultado</li>
     *           <li><b>tokenLength</b>: Longitud del token obtenido (solo en caso de éxito)</li>
     *           <li><b>tokenPreview</b>: Primeros 20 caracteres del token (solo en caso de éxito)</li>
     *           <li><b>errorType</b>: Tipo de excepción (solo en caso de error)</li>
     *         </ul>
     */
    @GetMapping("/spotify")
    public Map<String, Object> testSpotify() {
        Map<String, Object> response = new HashMap<>();

        try {
            String token = spotifyClient.getAccessToken();
            response.put("status", "success");
            response.put("api", "Spotify");
            response.put("message", "Conexión exitosa con Spotify API");
            response.put("tokenLength", token.length());
            response.put("tokenPreview", token.substring(0, Math.min(20, token.length())) + "...");
        } catch (Exception e) {
            log.error("Error al probar Spotify API", e);
            response.put("status", "error");
            response.put("api", "Spotify");
            response.put("message", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
        }

        return response;
    }

    /**
     * Prueba la conexión con Last.fm API.
     * <p>
     * Busca información de un artista para verificar que la API Key de Last.fm
     * está correctamente configurada y que la aplicación puede consultar datos.
     * </p>
     *
     * @param artist Nombre del artista a buscar (por defecto: "Radiohead")
     * @return Map con información del resultado:
     *         <ul>
     *           <li><b>status</b>: "success" o "error"</li>
     *           <li><b>api</b>: "Last.fm"</li>
     *           <li><b>message</b>: Mensaje descriptivo del resultado</li>
     *           <li><b>testedArtist</b>: Nombre del artista buscado (solo en caso de éxito)</li>
     *           <li><b>dataFields</b>: Número de campos en la respuesta (solo en caso de éxito)</li>
     *           <li><b>hasArtistData</b>: Sí se obtuvo información del artista (solo en caso de éxito)</li>
     *           <li><b>errorType</b>: Tipo de excepción (solo en caso de error)</li>
     *         </ul>
     */
    @GetMapping("/lastfm")
    public Map<String, Object> testLastFm(
            @RequestParam(defaultValue = "Radiohead") String artist
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> artistInfo = lastFmClient.getArtistInfo(artist);
            response.put("status", "success");
            response.put("api", "Last.fm");
            response.put("message", "Conexión exitosa con Last.fm API");
            response.put("testedArtist", artist);
            response.put("dataFields", artistInfo.keySet().size());
            response.put("hasArtistData", artistInfo.containsKey("artist"));
        } catch (Exception e) {
            log.error("Error al probar Last.fm API", e);
            response.put("status", "error");
            response.put("api", "Last.fm");
            response.put("message", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
        }

        return response;
    }

    /**
     * Prueba la conexión con TMDB (The Movie Database) API.
     * <p>
     * Busca una película para verificar que las credenciales de TMDB
     * (TMDB_API_KEY y TMDB_API_TOKEN) están correctamente configuradas
     * y que la aplicación puede consultar datos de películas.
     * </p>
     *
     * @param movie Título de la película a buscar (por defecto: "Inception")
     * @return Map con información del resultado:
     *         <ul>
     *           <li><b>status</b>: "success" o "error"</li>
     *           <li><b>api</b>: "TMDB"</li>
     *           <li><b>message</b>: Mensaje descriptivo del resultado</li>
     *           <li><b>searchQuery</b>: Término de búsqueda utilizado (solo en caso de éxito)</li>
     *           <li><b>dataFields</b>: Número de campos en la respuesta (solo en caso de éxito)</li>
     *           <li><b>hasResults</b>: Si se obtuvieron resultados (solo en caso de éxito)</li>
     *           <li><b>errorType</b>: Tipo de excepción (solo en caso de error)</li>
     *         </ul>
     */
    @GetMapping("/tmdb")
    public Map<String, Object> testTmdb(
            @RequestParam(defaultValue = "Inception") String movie
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> searchResults = tmdbClient.searchMovies(movie, 1);
            response.put("status", "success");
            response.put("api", "TMDB");
            response.put("message", "Conexión exitosa con TMDB API");
            response.put("searchQuery", movie);
            response.put("dataFields", searchResults.keySet().size());
            response.put("hasResults", searchResults.containsKey("results"));
        } catch (Exception e) {
            log.error("Error al probar TMDB API", e);
            response.put("status", "error");
            response.put("api", "TMDB");
            response.put("message", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
        }

        return response;
    }

    /**
     * Prueba todas las API externas simultáneamente.
     * <p>
     * Ejecuta pruebas de conectividad contra Spotify, Last.fm y TMDB
     * en paralelo, proporcionando un resumen del estado de todas las
     * integraciones en una sola petición.
     * </p>
     * <p>
     * Este endpoint es útil para:
     * <ul>
     *   <li>Verificación rápida de todas las integraciones</li>
     *   <li>Health checks completos del sistema</li>
     *   <li>Diagnóstico inicial de problemas de conectividad</li>
     * </ul>
     * </p>
     *
     * @return Map con información consolidada:
     *         <ul>
     *           <li><b>overallStatus</b>: "success" si todas funcionan, "partial" si alguna falla</li>
     *           <li><b>totalApis</b>: Número total de API probadas (3)</li>
     *           <li><b>successfulApis</b>: Número de API que respondieron correctamente</li>
     *           <li><b>results</b>: Map con resultados individuales de cada API:
     *             <ul>
     *               <li><b>Spotify</b>: Resultado de Spotify API</li>
     *               <li><b>Lastfm</b>: Resultado de Last.fm API</li>
     *               <li><b>Tmdb</b>: Resultado de TMDB API</li>
     *             </ul>
     *           </li>
     *         </ul>
     */
    @GetMapping("/all")
    public Map<String, Object> testAllApis() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> results = new HashMap<>();

        // Probar Spotify
        try {
            String token = spotifyClient.getAccessToken();
            results.put("spotify", Map.of(
                "status", "success",
                "tokenObtained", true
            ));
        } catch (Exception e) {
            results.put("spotify", Map.of(
                "status", "error",
                "error", e.getMessage()
            ));
        }

        // Probar Last.fm
        try {
            Map<String, Object> artistInfo = lastFmClient.getArtistInfo("The Beatles");
            results.put("lastfm", Map.of(
                "status", "success",
                "dataRetrieved", true
            ));
        } catch (Exception e) {
            results.put("lastfm", Map.of(
                "status", "error",
                "error", e.getMessage()
            ));
        }

        // Probar TMDB
        try {
            Map<String, Object> searchResults = tmdbClient.searchMovies("Matrix", 1);
            results.put("tmdb", Map.of(
                "status", "success",
                "dataRetrieved", true
            ));
        } catch (Exception e) {
            results.put("tmdb", Map.of(
                "status", "error",
                "error", e.getMessage()
            ));
        }

        // Calcular estado general
        long successCount = results.values().stream()
                .filter(r -> ((Map<?, ?>) r).get("status").equals("success"))
                .count();

        response.put("overallStatus", successCount == 3 ? "success" : "partial");
        response.put("totalApis", 3);
        response.put("successfulApis", successCount);
        response.put("results", results);

        return response;
    }
}

