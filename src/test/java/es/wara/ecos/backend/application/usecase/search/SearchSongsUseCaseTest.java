package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.application.service.SongIngestionService;
import es.wara.ecos.backend.domain.model.Page;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SemanticSearchProvider;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests de SearchSongsUseCase — estrategia BD-primero + on-demand para movie/q/artist.
 * La lógica de fetch por cuadrante se prueba en SongIngestionServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class SearchSongsUseCaseTest {

    @Mock SongRepository songRepository;
    @Mock SongIngestionService ingestionService;
    @Mock SemanticSearchProvider semanticSearchProvider;
    @InjectMocks SearchSongsUseCase useCase;

    private Song song(String title) {
        Song s = new Song(); s.setTitle(title); return s;
    }

    private static <T> Page<T> pageOf(List<T> items) {
        return new Page<>(items, 0, 20, items.size());
    }

    private static <T> Page<T> emptyPage() {
        return new Page<>(List.of(), 0, 20, 0);
    }

    private static <T> Page<T> fullPage(T item) {
        return new Page<>(Collections.nCopies(20, item), 0, 20, 20);
    }

    private SearchParams withQ(String q) {
        return SearchParams.builder().q(q).pageSize(20).build();
    }

    // ── BD suficiente → no llama a ingestion ─────────────────────────────────

    @Test
    void bdHasEnoughResults_returnsThemWithoutIngesting() {
        SearchParams p = withQ("Vangelis");
        when(ingestionService.summarize(p)).thenReturn("q=Vangelis");
        when(songRepository.search(p)).thenReturn(fullPage(song("Chariots of Fire")));

        Page<Song> result = useCase.execute(p);

        assertThat(result.items()).hasSize(20);
        verify(ingestionService, never()).ingest(any());
    }

    // ── Cuadrante on-demand: q ────────────────────────────────────────────────

    @Test
    void bdEmptyWithQ_callsIngestionAndReQueries() {
        SearchParams p = withQ("Vangelis");
        when(ingestionService.summarize(p)).thenReturn("q=Vangelis");
        when(songRepository.search(p))
                .thenReturn(emptyPage())
                .thenReturn(pageOf(List.of(song("Chariots of Fire"))));

        Page<Song> result = useCase.execute(p);

        verify(ingestionService).ingest(p);
        assertThat(result.items()).hasSize(1);
    }

    // ── Cuadrante on-demand: movie ────────────────────────────────────────────

    @Test
    void bdEmptyWithMovie_callsIngestion() {
        SearchParams p = SearchParams.builder().movie("Blade Runner").pageSize(20).build();
        when(ingestionService.summarize(p)).thenReturn("movie=Blade Runner");
        when(songRepository.search(p)).thenReturn(emptyPage()).thenReturn(emptyPage());

        useCase.execute(p);

        verify(ingestionService).ingest(p);
    }

    // ── Cuadrante on-demand: artist ───────────────────────────────────────────

    @Test
    void bdEmptyWithArtist_callsIngestion() {
        SearchParams p = SearchParams.builder().artist("Radiohead").pageSize(20).build();
        when(ingestionService.summarize(p)).thenReturn("artist=Radiohead");
        when(songRepository.search(p)).thenReturn(emptyPage()).thenReturn(emptyPage());

        useCase.execute(p);

        verify(ingestionService).ingest(p);
    }

    // ── Cuadrantes seed: NO disparan ingesta on-demand ────────────────────────

    @Test
    void bdEmptyWithGenre_doesNotCallIngestion() {
        SearchParams p = SearchParams.builder().genre("jazz").pageSize(20).build();
        when(ingestionService.summarize(p)).thenReturn("genre=jazz");
        when(songRepository.search(p)).thenReturn(emptyPage());

        useCase.execute(p);

        verify(ingestionService, never()).ingest(any());
    }

    @Test
    void bdEmptyWithAesthetic_doesNotCallIngestion() {
        SearchParams p = SearchParams.builder().aesthetic("dark academia").pageSize(20).build();
        when(ingestionService.summarize(p)).thenReturn("aesthetic=dark academia");
        when(songRepository.search(p)).thenReturn(emptyPage());

        useCase.execute(p);

        verify(ingestionService, never()).ingest(any());
    }

    @Test
    void bdEmptyWithDecade_doesNotCallIngestion() {
        SearchParams p = SearchParams.builder().decade("1990s").pageSize(20).build();
        when(ingestionService.summarize(p)).thenReturn("decade=1990s");
        when(songRepository.search(p)).thenReturn(emptyPage());

        useCase.execute(p);

        verify(ingestionService, never()).ingest(any());
    }

    @Test
    void bdEmptyWithCountry_doesNotCallIngestion() {
        SearchParams p = SearchParams.builder().country("Spain").pageSize(20).build();
        when(ingestionService.summarize(p)).thenReturn("country=Spain");
        when(songRepository.search(p)).thenReturn(emptyPage());

        useCase.execute(p);

        verify(ingestionService, never()).ingest(any());
    }

    // ── Boost semántico ───────────────────────────────────────────────────────

    @Test
    void semanticBoost_movesMatchingIdsToFront() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        UUID idC = UUID.randomUUID();

        Song a = songWithId(idA, "A");
        Song b = songWithId(idB, "B");
        Song c = songWithId(idC, "C");

        SearchParams p = withQ("dark ambient");
        when(ingestionService.summarize(p)).thenReturn("q=dark ambient");
        // BD devuelve A, B, C en ese orden
        when(songRepository.search(p)).thenReturn(pageOf(List.of(a, b, c)));
        // El micro dice que C e A son las más relevantes (C primero)
        when(semanticSearchProvider.search(eq("dark ambient"), anyInt()))
                .thenReturn(List.of(idC, idA));

        Page<Song> result = useCase.execute(p);

        // C debe subir primero (match semántico con mayor score), luego A, luego B
        assertThat(result.items()).extracting(Song::getId)
                .containsExactly(idC, idA, idB);
    }

    @Test
    void semanticBoost_noSemanticMatches_preservesOriginalOrder() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        Song a = songWithId(idA, "A");
        Song b = songWithId(idB, "B");

        SearchParams p = withQ("jazz");
        when(ingestionService.summarize(p)).thenReturn("q=jazz");
        when(songRepository.search(p)).thenReturn(pageOf(List.of(a, b)));
        // El micro devuelve un ID que no está en los resultados SQL
        when(semanticSearchProvider.search(anyString(), anyInt()))
                .thenReturn(List.of(UUID.randomUUID()));

        Page<Song> result = useCase.execute(p);

        assertThat(result.items()).extracting(Song::getId).containsExactly(idA, idB);
    }

    @Test
    void semanticBoost_microReturnsEmpty_preservesOriginalOrder() {
        UUID idA = UUID.randomUUID();
        Song a = songWithId(idA, "A");

        SearchParams p = withQ("chill");
        when(ingestionService.summarize(p)).thenReturn("q=chill");
        when(songRepository.search(p)).thenReturn(pageOf(List.of(a)));
        when(semanticSearchProvider.search(anyString(), anyInt())).thenReturn(List.of());

        Page<Song> result = useCase.execute(p);

        assertThat(result.items()).extracting(Song::getId).containsExactly(idA);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Song songWithId(UUID id, String title) {
        Song s = new Song();
        s.setId(id);
        s.setTitle(title);
        return s;
    }
}