package es.wara.ecos.backend.infrastructure.adapter;

import es.wara.ecos.backend.domain.model.Movie;
import es.wara.ecos.backend.infrastructure.adapter.deezer.DeezerClient;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.LastFmClient;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.MusicBrainzClient;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto.MusicBrainzReleaseGroupDTO;
import es.wara.ecos.backend.infrastructure.adapter.spotify.SpotifyClient;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.TmdbClient;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBCreditDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMovieDetailDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMultiResultDTO;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.dto.TMDBMultiSearchResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para la lógica cinematográfica de MusicProviderAdapter:
 * getMoviesForSong() con el nuevo endpoint searchMulti que soporta series.
 */
@ExtendWith(MockitoExtension.class)
class MusicProviderAdapterMovieTest {

    @Mock SpotifyClient spotifyClient;
    @Mock LastFmClient lastFmClient;
    @Mock TmdbClient tmdbClient;
    @Mock MusicBrainzClient musicBrainzClient;
    @Mock DeezerClient deezerClient;

    @InjectMocks MusicProviderAdapter adapter;

    // ─── helpers ──────────────────────────────────────────────────────────────

    private MusicBrainzReleaseGroupDTO releaseGroup(String title) {
        MusicBrainzReleaseGroupDTO rg = new MusicBrainzReleaseGroupDTO();
        rg.setId("mb-id-1");
        rg.setTitle(title);
        return rg;
    }

    private TMDBMultiResultDTO multiResult(Long id, String mediaType, String title, String date) {
        TMDBMultiResultDTO r = new TMDBMultiResultDTO();
        r.setId(id);
        r.setMediaType(mediaType);
        if ("movie".equals(mediaType)) {
            r.setTitle(title);
            r.setReleaseDate(date);
        } else {
            r.setName(title);
            r.setFirstAirDate(date);
        }
        return r;
    }

    private TMDBMultiSearchResponseDTO multiResponse(TMDBMultiResultDTO... results) {
        TMDBMultiSearchResponseDTO resp = new TMDBMultiSearchResponseDTO();
        resp.setResults(List.of(results));
        return resp;
    }

    // ─── getMoviesForSong — tipo movie ────────────────────────────────────────

    @Test
    void getMoviesForSong_movie_returnsMovieWithDirectorAndGenres() {
        when(musicBrainzClient.searchRecordingByArtistAndTitle("Vangelis", "Chariots of Fire"))
                .thenReturn(Optional.of("mb-rec-1"));
        when(musicBrainzClient.getMoviesForRecording("mb-rec-1"))
                .thenReturn(List.of(releaseGroup("Chariots of Fire")));
        when(tmdbClient.searchMulti("Chariots of Fire", 1))
                .thenReturn(multiResponse(multiResult(82L, "movie", "Chariots of Fire", "1981-03-30")));

        TMDBMovieDetailDTO detail = new TMDBMovieDetailDTO();
        TMDBMovieDetailDTO.GenreDTO genre = new TMDBMovieDetailDTO.GenreDTO();
        genre.setId(18);
        genre.setName("Drama");
        detail.setGenres(List.of(genre));
        when(tmdbClient.getMovieDetails(82L)).thenReturn(detail);

        TMDBCreditDTO credits = new TMDBCreditDTO();
        TMDBCreditDTO.CrewMember director = new TMDBCreditDTO.CrewMember();
        director.setJob("Director");
        director.setName("Hugh Hudson");
        credits.setCrew(List.of(director));
        when(tmdbClient.getMovieCredits(82L)).thenReturn(credits);

        List<Movie> result = adapter.getMoviesForSong("Vangelis", "Chariots of Fire");

        assertThat(result).hasSize(1);
        Movie movie = result.get(0);
        assertThat(movie.getTitle()).isEqualTo("Chariots of Fire");
        assertThat(movie.getYear()).isEqualTo(1981);
        assertThat(movie.getDirector()).isEqualTo("Hugh Hudson");
        assertThat(movie.getGenres()).containsExactly("Drama");
        assertThat(movie.getMediaType()).isEqualTo("movie");
        assertThat(movie.getTmdbId()).isEqualTo(82);
    }

    @Test
    void getMoviesForSong_tvShow_returnsSerieWithoutDirector() {
        when(musicBrainzClient.searchRecordingByArtistAndTitle("Survive", "Kyle"))
                .thenReturn(Optional.of("mb-rec-2"));
        when(musicBrainzClient.getMoviesForRecording("mb-rec-2"))
                .thenReturn(List.of(releaseGroup("Stranger Things")));
        when(tmdbClient.searchMulti("Stranger Things", 1))
                .thenReturn(multiResponse(multiResult(66732L, "tv", "Stranger Things", "2016-07-15")));

        List<Movie> result = adapter.getMoviesForSong("Survive", "Kyle");

        assertThat(result).hasSize(1);
        Movie movie = result.get(0);
        assertThat(movie.getTitle()).isEqualTo("Stranger Things");
        assertThat(movie.getYear()).isEqualTo(2016);
        assertThat(movie.getMediaType()).isEqualTo("tv");
        assertThat(movie.getDirector()).isNull();
        // No se llaman endpoints de película para series
        verify(tmdbClient, never()).getMovieCredits(any());
        verify(tmdbClient, never()).getMovieDetails(any());
    }

    @Test
    void getMoviesForSong_personResultFiltered_returnsEmpty() {
        TMDBMultiResultDTO person = new TMDBMultiResultDTO();
        person.setId(1L);
        person.setMediaType("person");
        person.setName("John Williams");

        when(musicBrainzClient.searchRecordingByArtistAndTitle("X", "Y"))
                .thenReturn(Optional.of("mb-rec-3"));
        when(musicBrainzClient.getMoviesForRecording("mb-rec-3"))
                .thenReturn(List.of(releaseGroup("John Williams OST")));
        when(tmdbClient.searchMulti("John Williams OST", 1))
                .thenReturn(multiResponse(person));

        List<Movie> result = adapter.getMoviesForSong("X", "Y");

        assertThat(result).isEmpty();
    }

    @Test
    void getMoviesForSong_noMbid_returnsEmpty() {
        when(musicBrainzClient.searchRecordingByArtistAndTitle(anyString(), anyString()))
                .thenReturn(Optional.empty());

        List<Movie> result = adapter.getMoviesForSong("Unknown", "Track");

        assertThat(result).isEmpty();
        verify(tmdbClient, never()).searchMulti(anyString(), anyInt());
    }

    @Test
    void getMoviesForSong_noSoundtracks_returnsEmpty() {
        when(musicBrainzClient.searchRecordingByArtistAndTitle(anyString(), anyString()))
                .thenReturn(Optional.of("mb-rec-4"));
        when(musicBrainzClient.getMoviesForRecording("mb-rec-4"))
                .thenReturn(List.of());

        List<Movie> result = adapter.getMoviesForSong("Artist", "Title");

        assertThat(result).isEmpty();
        verify(tmdbClient, never()).searchMulti(anyString(), anyInt());
    }
}