package es.wara.ecos.backend.application.service;

import es.wara.ecos.backend.application.usecase.search.SearchParams;
import es.wara.ecos.backend.domain.model.Artist;
import es.wara.ecos.backend.domain.model.Song;import es.wara.ecos.backend.domain.port.EmbeddingProvider;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongIngestionServiceTest {

    @Mock MusicProvider musicProvider;
    @Mock MusicAggregationService musicAggregationService;
    @Mock SongRepository songRepository;
    @Mock EmbeddingProvider embeddingProvider;

    SongIngestionService service;

    @BeforeEach
    void setUp() {
        service = new SongIngestionService(musicProvider, musicAggregationService, songRepository, embeddingProvider);
        lenient().when(musicAggregationService.enrich(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Song song(String title) {
        Song s = new Song(); s.setTitle(title); return s;
    }

    // ── Jerarquía de fuentes ─────────────────────────────────────────────────

    @Test
    void movie_callsSoundtrackSongs() {
        SearchParams p = SearchParams.builder().movie("Blade Runner").pageSize(20).build();
        when(musicProvider.getSoundtrackSongs("Blade Runner")).thenReturn(List.of(song("Tears in Rain")));

        service.ingest(p);

        verify(musicProvider).getSoundtrackSongs("Blade Runner");
    }

    @Test
    void country_callsGetArtistsByCountryThenSearchesSongs() {
        SearchParams p = SearchParams.builder().country("Spain").pageSize(20).build();
        when(musicProvider.getArtistNamesByCountry("Spain", 5)).thenReturn(List.of("Rosalía"));
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        service.ingest(p);

        verify(musicProvider).getArtistNamesByCountry("Spain", 5);
        verify(musicProvider).searchSongs("artist:Rosalía", 20);
    }

    @Test
    void country_noArtistsFound_fallsBackToDirectSearch() {
        SearchParams p = SearchParams.builder().country("Japan").pageSize(20).build();
        when(musicProvider.getArtistNamesByCountry("Japan", 5)).thenReturn(List.of());
        when(musicProvider.searchSongs("Japan", 20)).thenReturn(List.of());

        service.ingest(p);

        verify(musicProvider).searchSongs("Japan", 20);
    }

    @Test
    void country_injectsCountryOnMatchingArtist() {
        Artist artist = new Artist();
        artist.setName("Rosalía");
        Song s = song("Malamente");
        s.setArtists(new ArrayList<>(List.of(artist)));

        SearchParams p = SearchParams.builder().country("Spain").pageSize(20).build();
        when(musicProvider.getArtistNamesByCountry("Spain", 5)).thenReturn(List.of("Rosalía"));
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of(s));

        service.ingest(p);

        verify(songRepository).save(argThat(saved ->
                saved.getArtists().stream()
                        .filter(a -> "Rosalía".equals(a.getName()))
                        .anyMatch(a -> "Spain".equals(a.getCountry()))
        ));
    }

    @Test
    void aesthetic_callsSearchSongsByTag() {
        SearchParams p = SearchParams.builder().aesthetic("dark academia").pageSize(20).build();
        when(musicProvider.searchSongsByTag("dark academia", 20)).thenReturn(List.of());

        service.ingest(p);

        verify(musicProvider).searchSongsByTag("dark academia", 20);
    }

    @Test
    void aestheticWithDecade_usesSpotifyQuery() {
        SearchParams p = SearchParams.builder().aesthetic("lofi").decade("1990s").pageSize(20).build();
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        service.ingest(p);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(captor.capture(), eq(20));
        assertThat(captor.getValue()).isEqualTo("year:1990-1999");
    }

    @Test
    void genre_callsLastFm() {
        SearchParams p = SearchParams.builder().genre("jazz").pageSize(20).build();
        when(musicProvider.searchSongsByTag("jazz", 20)).thenReturn(List.of(song("Kind of Blue")));

        service.ingest(p);

        verify(musicProvider).searchSongsByTag("jazz", 20);
        verify(musicProvider, never()).searchSongs(anyString(), anyInt());
    }

    @Test
    void genreWithArtist_usesSpotifyQuery() {
        SearchParams p = SearchParams.builder().genre("jazz").artist("Coltrane").pageSize(20).build();
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        service.ingest(p);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(captor.capture(), anyInt());
        assertThat(captor.getValue()).contains("artist:Coltrane").contains("genre:jazz");
    }

    @Test
    void decade_buildsYearRangeQuery() {
        SearchParams p = SearchParams.builder().decade("1980s").pageSize(20).build();
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        service.ingest(p);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(captor.capture(), anyInt());
        assertThat(captor.getValue()).isEqualTo("year:1980-1989");
    }

    @Test
    void textAndDecade_combinesQueryAndYearRange() {
        SearchParams p = SearchParams.builder().q("Vangelis").decade("1980s").pageSize(20).build();
        when(musicProvider.searchSongs(anyString(), anyInt())).thenReturn(List.of());

        service.ingest(p);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(musicProvider).searchSongs(captor.capture(), anyInt());
        assertThat(captor.getValue()).isEqualTo("Vangelis year:1980-1989");
    }

    // ── ensureSearchTags ──────────────────────────────────────────────────────

    @Test
    void ensureSearchTags_addsAestheticTagIfMissing() {
        SearchParams p = SearchParams.builder().aesthetic("cottagecore").pageSize(20).build();
        Song s = song("Test");
        when(musicProvider.searchSongsByTag(anyString(), anyInt())).thenReturn(List.of(s));

        service.ingest(p);

        verify(songRepository).save(argThat(saved ->
                saved.getTags() != null &&
                saved.getTags().stream().anyMatch(t -> "cottagecore".equals(t.getName()))
        ));
    }

    @Test
    void ensureSearchTags_doesNotDuplicateExistingTag() {
        SearchParams p = SearchParams.builder().genre("indie").pageSize(20).build();
        Song s = song("Float On");
        when(musicProvider.searchSongsByTag("indie", 20)).thenReturn(List.of(s));
        // Simula que enrich ya añadió el tag
        when(musicAggregationService.enrich(any())).thenAnswer(inv -> {
            Song song = inv.getArgument(0);
            var tag = new es.wara.ecos.backend.domain.model.Tag();
            tag.setName("indie");
            tag.setWeight(java.math.BigDecimal.valueOf(0.9));
            song.setTags(new ArrayList<>(List.of(tag)));
            return song;
        });

        service.ingest(p);

        verify(songRepository).save(argThat(saved ->
                saved.getTags().stream().filter(t -> "indie".equals(t.getName())).count() == 1
        ));
    }
}