package es.wara.ecos.backend.infrastructure.adapter.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO que representa una playlist creada o recuperada de Spotify.
 * <p>
 * Se usa al exportar playlists de ECOS a Spotify:
 * {@code POST /v1/users/{user_id}/playlists}
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyPlaylistDTO {

    /** Identificador único de la playlist en Spotify. */
    private String id;

    /** Nombre de la playlist. */
    private String name;

    /** Descripción opcional de la playlist. */
    private String description;

    /** URLs externas de la playlist (incluye la URL web de Spotify). */
    @JsonProperty("external_urls")
    private Map<String, String> externalUrls;

    /** URI de Spotify para abrir la playlist en la app (ej. {@code spotify:playlist:xxx}). */
    private String uri;
}