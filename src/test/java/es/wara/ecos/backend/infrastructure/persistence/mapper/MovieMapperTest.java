package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.Movie;
import es.wara.ecos.backend.infrastructure.persistence.entity.MovieEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MovieMapperTest {

    private final MovieMapper mapper = new MovieMapper();

    // ─── toModel ──────────────────────────────────────────────────────────────

    @Test
    void toModel_movie_mapsAllFields() {
        UUID id = UUID.randomUUID();
        MovieEntity entity = new MovieEntity();
        entity.setId(id);
        entity.setTmdbId(550);
        entity.setTitle("Fight Club");
        entity.setYear((short) 1999);
        entity.setDirector("David Fincher");
        entity.setGenres(List.of("Drama", "Thriller"));
        entity.setMediaType("movie");

        Movie model = mapper.toModel(entity);

        assertThat(model.getId()).isEqualTo(id);
        assertThat(model.getTmdbId()).isEqualTo(550);
        assertThat(model.getTitle()).isEqualTo("Fight Club");
        assertThat(model.getYear()).isEqualTo(1999);
        assertThat(model.getDirector()).isEqualTo("David Fincher");
        assertThat(model.getGenres()).containsExactly("Drama", "Thriller");
        assertThat(model.getMediaType()).isEqualTo("movie");
    }

    @Test
    void toModel_tv_mapsMediaType() {
        MovieEntity entity = new MovieEntity();
        entity.setTmdbId(1399);
        entity.setTitle("Game of Thrones");
        entity.setYear((short) 2011);
        entity.setMediaType("tv");

        Movie model = mapper.toModel(entity);

        assertThat(model.getTitle()).isEqualTo("Game of Thrones");
        assertThat(model.getMediaType()).isEqualTo("tv");
    }

    @Test
    void toModel_null_returnsNull() {
        assertThat(mapper.toModel(null)).isNull();
    }

    // ─── toEntity ─────────────────────────────────────────────────────────────

    @Test
    void toEntity_movie_mapsAllFields() {
        Movie model = new Movie(null, 278, "The Shawshank Redemption", 1994, "Frank Darabont",
                List.of("Drama"), "movie");

        MovieEntity entity = mapper.toEntity(model);

        assertThat(entity.getTmdbId()).isEqualTo(278);
        assertThat(entity.getTitle()).isEqualTo("The Shawshank Redemption");
        assertThat(entity.getYear()).isEqualTo((short) 1994);
        assertThat(entity.getDirector()).isEqualTo("Frank Darabont");
        assertThat(entity.getGenres()).containsExactly("Drama");
        assertThat(entity.getMediaType()).isEqualTo("movie");
    }

    @Test
    void toEntity_tv_mapsMediaType() {
        Movie model = new Movie(null, 1399, "Game of Thrones", 2011, null, List.of(), "tv");

        MovieEntity entity = mapper.toEntity(model);

        assertThat(entity.getMediaType()).isEqualTo("tv");
    }

    @Test
    void toEntity_nullMediaType_defaultsToMovie() {
        Movie model = new Movie(null, 123, "Sin Tipo", 2020, null, null, null);

        MovieEntity entity = mapper.toEntity(model);

        assertThat(entity.getMediaType()).isEqualTo("movie");
    }

    @Test
    void toEntity_null_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}