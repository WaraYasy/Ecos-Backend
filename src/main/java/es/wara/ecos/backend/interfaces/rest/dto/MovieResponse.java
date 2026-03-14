package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.Movie;

/**
 * DTO de respuesta para una película asociada a una canción.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record MovieResponse(
        Integer tmdbId,
        String title,
        Integer year,
        String director
) {

    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.getTmdbId(),
                movie.getTitle(),
                movie.getYear(),
                movie.getDirector()
        );
    }

}
