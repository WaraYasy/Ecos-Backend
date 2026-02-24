package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.Movie;
import es.wara.ecos.backend.infrastructure.persistence.entity.MovieEntity;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper {

    public Movie toModel(MovieEntity entity) {
        if (entity == null) return null;
        return new Movie(
                entity.getId(),
                entity.getTmdbId(),
                entity.getTitle(),
                entity.getYear() != null ? entity.getYear().intValue() : null,
                entity.getDirector()
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
        return entity;
    }

}