package es.wara.ecos.backend.infrastructure.adapter.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta de detalles completos de una película en TMDb.
 * <p>
 * Se deserializa desde {@code GET /movie/{id}}.
 * A diferencia de {@link TMDBMovieDTO} (usado en búsqueda, que devuelve solo IDs de géneros),
 * este DTO contiene los géneros con nombre completo.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TMDBMovieDetailDTO {

    /** Identificador único de la película en TMDb. */
    private Long id;

    /** Título de la película. */
    private String title;

    /** Sinopsis de la película. */
    private String overview;

    /** Fecha de estreno en formato "YYYY-MM-DD". */
    @JsonProperty("release_date")
    private String releaseDate;

    /** Géneros cinematográficos con id y nombre completo. */
    private List<GenreDTO> genres;

    /** Ruta relativa del póster. */
    @JsonProperty("poster_path")
    private String posterPath;

    /**
     * Género cinematográfico de TMDb con id y nombre completo.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenreDTO {
        private Integer id;
        private String name;
    }

}