package es.wara.ecos.backend.application.service;

import es.wara.ecos.backend.infrastructure.adapter.tmdb.TmdbClient;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMultiResultDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMultiSearchResponseDTO;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaArtistRepository;
import es.wara.ecos.backend.infrastructure.seed.SeedTermsProperties;
import es.wara.ecos.backend.interfaces.rest.dto.SuggestionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuggestionsServiceTest {

    @Mock TmdbClient tmdbClient;
    @Mock SeedTermsProperties terms;
    @Mock JpaArtistRepository artistRepository;
    @InjectMocks SuggestionsService service;

    // ─── helpers ──────────────────────────────────────────────────────────────

    private TMDBMultiResultDTO movieResult(String title, String releaseDate) {
        TMDBMultiResultDTO r = new TMDBMultiResultDTO();
        r.setId(1L);
        r.setMediaType("movie");
        r.setTitle(title);
        r.setReleaseDate(releaseDate);
        return r;
    }

    private TMDBMultiResultDTO tvResult(String name, String firstAirDate) {
        TMDBMultiResultDTO r = new TMDBMultiResultDTO();
        r.setId(2L);
        r.setMediaType("tv");
        r.setName(name);
        r.setFirstAirDate(firstAirDate);
        return r;
    }

    private TMDBMultiResultDTO personResult() {
        TMDBMultiResultDTO r = new TMDBMultiResultDTO();
        r.setId(3L);
        r.setMediaType("person");
        r.setName("Jon Snow");
        return r;
    }

    private TMDBMultiSearchResponseDTO response(TMDBMultiResultDTO... results) {
        TMDBMultiSearchResponseDTO resp = new TMDBMultiSearchResponseDTO();
        resp.setResults(List.of(results));
        return resp;
    }

    // ─── suggestMovies ─────────────────────────────────────────────────────────

    @Test
    void suggestMovies_movieResult_returnsLabelWithYear() {
        when(tmdbClient.searchMulti(eq("avengers"), anyInt()))
                .thenReturn(response(movieResult("The Avengers", "2012-05-04")));

        List<SuggestionResponse> result = service.suggestMovies("avengers");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).value()).isEqualTo("The Avengers");
        assertThat(result.get(0).label()).isEqualTo("The Avengers (2012)");
    }

    @Test
    void suggestMovies_tvResult_returnsLabelWithYearAndSerieSuffix() {
        when(tmdbClient.searchMulti(eq("tronos"), anyInt()))
                .thenReturn(response(tvResult("Juego de Tronos", "2011-04-17")));

        List<SuggestionResponse> result = service.suggestMovies("tronos");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).value()).isEqualTo("Juego de Tronos");
        assertThat(result.get(0).label()).isEqualTo("Juego de Tronos (2011) · Serie");
    }

    @Test
    void suggestMovies_mixedResults_includesBothMoviesAndSeries() {
        when(tmdbClient.searchMulti(eq("game"), anyInt()))
                .thenReturn(response(
                        movieResult("Game Night", "2018-02-23"),
                        tvResult("Game of Thrones", "2011-04-17")
                ));

        List<SuggestionResponse> result = service.suggestMovies("game");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).label()).isEqualTo("Game Night (2018)");
        assertThat(result.get(1).label()).isEqualTo("Game of Thrones (2011) · Serie");
    }

    @Test
    void suggestMovies_personResultFiltered_notIncluded() {
        when(tmdbClient.searchMulti(eq("jon"), anyInt()))
                .thenReturn(response(personResult()));

        List<SuggestionResponse> result = service.suggestMovies("jon");

        assertThat(result).isEmpty();
    }

    @Test
    void suggestMovies_resultWithoutYear_labelHasNoYear() {
        TMDBMultiResultDTO noDate = tvResult("Miniserie Sin Fecha", null);
        when(tmdbClient.searchMulti(eq("mini"), anyInt()))
                .thenReturn(response(noDate));

        List<SuggestionResponse> result = service.suggestMovies("mini");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).label()).isEqualTo("Miniserie Sin Fecha · Serie");
    }

    @Test
    void suggestMovies_shortQuery_returnsEmptyWithoutCallingApi() {
        List<SuggestionResponse> result = service.suggestMovies("a");

        assertThat(result).isEmpty();
        verify(tmdbClient, never()).searchMulti(eq("a"), anyInt());
    }

    @Test
    void suggestMovies_nullQuery_returnsEmpty() {
        assertThat(service.suggestMovies(null)).isEmpty();
        verify(tmdbClient, never()).searchMulti(eq(null), anyInt());
    }

    @Test
    void suggestMovies_apiThrowsException_returnsEmpty() {
        when(tmdbClient.searchMulti(eq("error"), anyInt()))
                .thenThrow(new RuntimeException("TMDB down"));

        assertThat(service.suggestMovies("error")).isEmpty();
    }

    @Test
    void suggestMovies_limitsToSixResults() {
        TMDBMultiSearchResponseDTO resp = new TMDBMultiSearchResponseDTO();
        resp.setResults(List.of(
                tvResult("Serie 1", "2010-01-01"),
                tvResult("Serie 2", "2011-01-01"),
                tvResult("Serie 3", "2012-01-01"),
                tvResult("Serie 4", "2013-01-01"),
                tvResult("Serie 5", "2014-01-01"),
                tvResult("Serie 6", "2015-01-01"),
                tvResult("Serie 7", "2016-01-01")
        ));
        when(tmdbClient.searchMulti(eq("serie"), anyInt())).thenReturn(resp);

        List<SuggestionResponse> result = service.suggestMovies("serie");

        assertThat(result).hasSize(6);
    }
}