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
 * Controlador de prueba para verificar la conectividad con APIs externas
 * Endpoints disponibles:
 * - GET /api/test/spotify - Prueba conexión con Spotify
 * - GET /api/test/lastfm - Prueba conexión con Last.fm
 * - GET /api/test/tmdb - Prueba conexión con TMDB
 * - GET /api/test/all - Prueba todas las APIs
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
     * Prueba la conexión con Spotify API
     * Obtiene un access token para verificar que las credenciales funcionan
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
     * Prueba la conexión con Last.fm API
     * Busca información de un artista de prueba
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
     * Prueba la conexión con TMDB API
     * Busca una película de prueba
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
     * Prueba todas las APIs de una vez
     * Útil para verificar que todas las integraciones funcionan correctamente
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

