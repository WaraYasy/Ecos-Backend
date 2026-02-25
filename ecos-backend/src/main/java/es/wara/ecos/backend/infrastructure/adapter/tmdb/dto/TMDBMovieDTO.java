package es.wara.ecos.backend.infrastructure.adapter.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa una película de la API de TMDb.
 * <p>
 * Se usa en resultados de búsqueda ({@link TMDBSearchResponseDTO}) y en
 * detalles de película ({@code GET /movie/{id}}).
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TMDBMovieDTO {

    /** Identificador único de la película en TMDb. */
    private Long id;

    /** Título de la película. */
    private String title;

    /** Sinopsis de la película. */
    private String overview;

    /** Fecha de estreno en formato "YYYY-MM-DD". */
    @JsonProperty("release_date")
    private String releaseDate;

    /** IDs de géneros cinematográficos (en resultados de búsqueda). */
    @JsonProperty("genre_ids")
    private List<Integer> genreIds;

    /** Puntuación media de los usuarios en TMDb (0-10). */
    @JsonProperty("vote_average")
    private Double voteAverage;

    /** Ruta relativa del póster (se concatena con la base URL de imágenes de TMDb). */
    @JsonProperty("poster_path")
    private String posterPath;

    /** Idioma original de la película (código ISO 639-1). */
    @JsonProperty("original_language")
    private String originalLanguage;
}