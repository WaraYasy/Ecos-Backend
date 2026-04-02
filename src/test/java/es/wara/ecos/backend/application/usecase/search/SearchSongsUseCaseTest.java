package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.application.service.MusicAggregationService;
import es.wara.ecos.backend.domain.model.Page;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchSongsUseCaseTest {

    @Mock SongRepository songRepository;
    @Mock MusicProvider musicProvider;
    @Mock MusicAggregationService musicAggregationService;
    @InjectMocks SearchSongsUseCase useCase;

    @BeforeEach
    void stubEnrich() {
        // lenient: no todos los tests llaman a enrich() (los que van por BD no lo hacen)
        lenient().when(musicAggregationService.enrich(any(Song.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Song song(String title) {
        Song s = new Song();
        s.setTitle(title);
        s.setSpotifyId("spotify_" + title.toLowerCase().replace(" ", "_"));
        return s;
    }

    private SearchParams params(String q) {
        return new SearchParams(q, null, null, null, null, null, null, null,
                null, null, null, null, null, null, 20, 0);
    }

    private static <T> Page<T> pageOf(List<T> items) {
        return new Page<>(items, 0, 20, items.size());
    }

    private static <T> Page<T> emptyPage() {
        return new Page<>(List.of(), 0, 20, 0);
    }

    /** Página completa (pageSize=20): satisface el umbral mínimo Y no dispara background refresh. */
    private static <T> Page<T> fullPage(T item) {
        List<T> items = Collections.nCopies(20, item);
        return new Page<>(items, 0, 20, 20);
    }

    // ── estrategia BD-primero ────────────────────────────────────────────────

    @Test
    void execute_bdHasResults_returnsThemWithoutCallingExternalApis() {
        SearchParams p = params("Vangelis");
        // Página completa: satisface el umbral mínimo (>=5) y no dispara background refresh
        when(songRepository.search(p)).thenReturn(fullPage(song("Chariots of Fire")));

        Page<Song> result = useCase.execute(p);

        assertThat(result.items()).hasSize(20);
        verify(musicProvider, never()).searchSongs(anyString(), anyInt());
    }

    @Test
    void execute_bdEmptyWithTextQuery_callsSpotifySearchAndPersists() {
        SearchParams p = params("Vangelis");
        when(songRepository.search(p))
                .thenReturn(emptyPage())
                .thenReturn(pageOf(List.of(song("X"))));
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of(song("X")));

        Page<Song> result = useCase.execute(p);

        assertThat(result.items()).hasSize(1);
        verify(musicProvider).searchSongs("Vangelis", 20);
        verify(songRepository).save(any(Song.class));
    }

    @Test
    void execute_bdEmptyWithOnlyTechnicalQuadrant_returnsEmptyWithoutApiCall() {
        // Solo cuadrante técnico (energyMin) — no hay q, aesthetic, genre, decade ni movie
        SearchParams p = new SearchParams(null, null, null, null, null, null, null, null,
                new java.math.BigDecimal("0.5"), null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(emptyPage());

        Page<Song> result = useCase.execute(p);

        assertThat(result.items()).isEmpty();
        verify(musicProvider, never()).searchSongs(anyString(), anyInt());
        verify(musicProvider, never()).searchSongsByTag(anyString(), anyInt());
        verify(musicProvider, never()).getSoundtrackSongs(anyString());
    }

    // ── construcción de query Spotify por cuadrante ──────────────────────────

    @Test
    void execute_decadeOnly_buildsYearRangeQuery() {
        SearchParams p = new SearchParams(null, null, null, "1980s", null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(emptyPage()).thenReturn(emptyPage());
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        useCase.execute(p);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(queryCaptor.capture(), anyInt());
        assertThat(queryCaptor.getValue()).isEqualTo("year:1980-1989");
    }

    @Test
    void execute_textAndDecade_combinesQueryWithYearRange() {
        SearchParams p = new SearchParams("Vangelis", null, null, "1980s", null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(emptyPage()).thenReturn(emptyPage());
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        useCase.execute(p);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(queryCaptor.capture(), anyInt());
        assertThat(queryCaptor.getValue()).isEqualTo("Vangelis year:1980-1989");
    }

    @Test
    void execute_aestheticQuadrant_callsSearchByTag() {
        SearchParams p = new SearchParams(null, "dark academia", null, null, null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(emptyPage()).thenReturn(emptyPage());
        when(musicProvider.searchSongsByTag("dark academia", 20)).thenReturn(List.of());

        useCase.execute(p);

        verify(musicProvider).searchSongsByTag("dark academia", 20);
    }

    @Test
    void execute_movieQuadrant_callsGetSoundtrackSongs() {
        SearchParams p = new SearchParams(null, null, null, null, "Blade Runner", null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(emptyPage()).thenReturn(emptyPage());
        when(musicProvider.getSoundtrackSongs("Blade Runner")).thenReturn(List.of());

        useCase.execute(p);

        verify(musicProvider).getSoundtrackSongs("Blade Runner");
    }

    // ── cuadrante género ─────────────────────────────────────────────────────

    @Test
    void execute_genreOnly_buildsSpotifyQueryWithGenreFilter() {
        SearchParams p = new SearchParams(null, null, "rock", null, null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(emptyPage()).thenReturn(emptyPage());
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        useCase.execute(p);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(queryCaptor.capture(), anyInt());
        assertThat(queryCaptor.getValue()).isEqualTo("genre:rock");
    }

    @Test
    void execute_genreAndText_combinesQueryAndGenreFilter() {
        SearchParams p = new SearchParams("Miles Davis", null, "jazz", null, null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(emptyPage()).thenReturn(emptyPage());
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        useCase.execute(p);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(queryCaptor.capture(), anyInt());
        assertThat(queryCaptor.getValue()).isEqualTo("Miles Davis genre:jazz");
    }

    @Test
    void execute_genreAndDecade_combinesGenreAndYearRange() {
        SearchParams p = new SearchParams(null, null, "jazz", "1960s", null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(emptyPage()).thenReturn(emptyPage());
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        useCase.execute(p);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(queryCaptor.capture(), anyInt());
        assertThat(queryCaptor.getValue()).isEqualTo("genre:jazz year:1960-1969");
    }

    @Test
    void execute_genreWithBdResults_doesNotCallExternalApis() {
        SearchParams p = new SearchParams(null, null, "rock", null, null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        // Página completa: satisface el umbral mínimo (>=5) y no dispara background refresh
        when(songRepository.search(p)).thenReturn(fullPage(song("Bohemian Rhapsody")));

        Page<Song> result = useCase.execute(p);

        assertThat(result.items()).hasSize(20);
        verify(musicProvider, never()).searchSongs(anyString(), anyInt());
        verify(musicProvider, never()).searchSongsByTag(anyString(), anyInt());
    }

    @Test
    void execute_genreEnsuresTagOnExternalSongs() {
        SearchParams p = new SearchParams(null, null, "indie", null, null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        Song externalSong = song("Float On");
        when(songRepository.search(p))
                .thenReturn(emptyPage())
                .thenReturn(pageOf(List.of(externalSong)));
        when(musicProvider.searchSongs("genre:indie", 20)).thenReturn(List.of(externalSong));

        useCase.execute(p);

        // ensureSearchTags debe haber añadido el tag "indie" al song
        verify(songRepository).save(argThat(s ->
                s.getTags() != null && s.getTags().stream().anyMatch(t -> "indie".equals(t.getName()))
        ));
    }
}
