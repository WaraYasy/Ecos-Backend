package es.wara.ecos.backend.infrastructure.adapter.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa una imagen de Spotify (portada de álbum, foto de artista, etc.).
 * <p>
 * Las imágenes en Spotify vienen siempre en lista, ordenadas de mayor a menor resolución.
 * La primera imagen de la lista es la de mayor calidad.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyImageDTO {

    /** URL pública de la imagen. */
    private String url;

    /** Altura en píxeles (puede ser null para imágenes de tamaño variable). */
    private Integer height;

    /** Anchura en píxeles (puede ser null para imágenes de tamaño variable). */
    private Integer width;
}