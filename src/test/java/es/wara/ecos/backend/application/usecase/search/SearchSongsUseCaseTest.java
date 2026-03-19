package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.application.service.MusicAggregationService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
        return s;
    }

    private SearchParams params(String q) {
        return new SearchParams(q, null, null, null, null, null, null,
                null, null, null, null, null, null, 20, 0);
    }

    // ── estrategia BD-primero ────────────────────────────────────────────────

    @Test
    void execute_bdHasResults_returnsThemWithoutCallingExternalApis() {
        SearchParams p = params("Vangelis");
        when(songRepository.search(p)).thenReturn(List.of(song("Chariots of Fire")));

        List<Song> result = useCase.execute(p);

        assertThat(result).hasSize(1);
        verify(musicProvider, never()).searchSongs(anyString(), anyInt());
    }

    @Test
    void execute_bdEmptyWithTextQuery_callsSpotifySearchAndPersists() {
        SearchParams p = params("Vangelis");
        when(songRepository.search(p))
                .thenReturn(List.of())          // primera llamada: vacío
                .thenReturn(List.of(song("X"))); // segunda tras persistir
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of(song("X")));

        List<Song> result = useCase.execute(p);

        assertThat(result).hasSize(1);
        verify(musicProvider).searchSongs("Vangelis", 20);
        verify(songRepository).save(any(Song.class));
    }

    @Test
    void execute_bdEmptyWithOnlyTechnicalQuadrant_returnsEmptyWithoutApiCall() {
        // Solo cuadrante técnico (energyMin) — no hay q, aesthetic, decade ni movie
        SearchParams p = new SearchParams(null, null, null, null, null, null, null,
                new java.math.BigDecimal("0.5"), null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(List.of());

        List<Song> result = useCase.execute(p);

        assertThat(result).isEmpty();
        verify(musicProvider, never()).searchSongs(anyString(), anyInt());
        verify(musicProvider, never()).searchSongsByTag(anyString(), anyInt());
        verify(musicProvider, never()).getSoundtrackSongs(anyString());
    }

    // ── construcción de query Spotify por cuadrante ──────────────────────────

    @Test
    void execute_decadeOnly_buildsYearRangeQuery() {
        SearchParams p = new SearchParams(null, null, "1980s", null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(List.of()).thenReturn(List.of());
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        useCase.execute(p);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(queryCaptor.capture(), anyInt());
        assertThat(queryCaptor.getValue()).isEqualTo("year:1980-1989");
    }

    @Test
    void execute_textAndDecade_combinesQueryWithYearRange() {
        SearchParams p = new SearchParams("Vangelis", null, "1980s", null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(List.of()).thenReturn(List.of());
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        useCase.execute(p);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(queryCaptor.capture(), anyInt());
        assertThat(queryCaptor.getValue()).isEqualTo("Vangelis year:1980-1989");
    }

    @Test
    void execute_aestheticQuadrant_callsSearchByTag() {
        SearchParams p = new SearchParams(null, "dark academia", null, null, null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(List.of()).thenReturn(List.of());
        when(musicProvider.searchSongsByTag("dark academia", 20)).thenReturn(List.of());

        useCase.execute(p);

        verify(musicProvider).searchSongsByTag("dark academia", 20);
    }

    @Test
    void execute_movieQuadrant_callsGetSoundtrackSongs() {
        SearchParams p = new SearchParams(null, null, null, "Blade Runner", null, null, null,
                null, null, null, null, null, null, 20, 0);
        when(songRepository.search(p)).thenReturn(List.of()).thenReturn(List.of());
        when(musicProvider.getSoundtrackSongs("Blade Runner")).thenReturn(List.of());

        useCase.execute(p);

        verify(musicProvider).getSoundtrackSongs("Blade Runner");
    }

}