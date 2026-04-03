package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.Movie;
import es.wara.ecos.backend.infrastructure.persistence.entity.MovieEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper que convierte entre MovieEntity y el modelo de dominio Movie.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class MovieMapper {

    public Movie toModel(MovieEntity entity) {
        if (entity == null) return null;
        return new Movie(
                entity.getId(),
                entity.getTmdbId(),
                entity.getTitle(),
                entity.getYear() != null ? entity.getYear().intValue() : null,
                entity.getDirector(),
                entity.getGenres(),
                entity.getMediaType()
        );
    }

    public MovieEntity toEntity(Movie model) {
        if (model == null) return null;
        MovieEntity entity = new MovieEntity();
        entity.setId(model.getId());
        entity.setTmdbId(model.getTmdbId());
        entity.setTitle(model.getTitle());
        entity.setYear(model.getYear() != null ? model.getYear().shortValue() : null);
        entity.setDirector(model.getDirector());
        entity.setGenres(model.getGenres());
        entity.setMediaType(model.getMediaType() != null ? model.getMediaType() : "movie");
        return entity;
    }

}
