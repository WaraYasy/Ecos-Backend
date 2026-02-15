package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.infrastructure.adapter.spotify.SpotifyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")

public class TestController {

        @Autowired
        private SpotifyClient spotifyClient;

        @GetMapping("/spotify")
        public Map<String, String> testSpotify() {
            Map<String, String> response = new HashMap<>();

            try {
                String token = spotifyClient.getAccessToken();
                response.put("status", "success");
                response.put("token", token.substring(0, 20) + "...");
                response.put("message", "Conexión exitosa con Spotify API");
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", e.getMessage());
            }

            return response;
        }
}
