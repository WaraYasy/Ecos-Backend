package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.domain.model.Artist;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.SongEmbedding;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongEmbeddingRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSimilarSongsUseCaseTest {

    @Mock SongRepository songRepository;
    @Mock SongEmbeddingRepository embeddingRepository;
    @Mock MusicProvider musicProvider;
    @InjectMocks GetSimilarSongsUseCase useCase;

    private static final UUID SONG_ID    = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID NEIGHBOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID UNKNOWN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private Song buildSong(UUID id, String title, String artistName) {
        Song s = new Song();
        s.setId(id);
        s.setTitle(title);
        if (artistName != null) {
            Artist artist = new Artist();
            artist.setName(artistName);
            s.setArtists(List.of(artist));
        }
        return s;
    }

    private SongEmbedding buildEmbedding(UUID songId) {
        SongEmbedding e = new SongEmbedding();
        e.setSongId(songId);
        e.setEmbedding(new float[384]);
        return e;
    }

    // ── Estrategia primaria: embeddings ──────────────────────────────────────

    @Test
    void execute_songWithEmbedding_returnsNeighborsFromPgvector() {
        Song song = buildSong(SONG_ID, "Chariots of Fire", "Vangelis");
        Song neighbor = buildSong(NEIGHBOR_ID, "Pulstar", "Vangelis");

        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(embeddingRepository.findBySongId(SONG_ID)).thenReturn(Optional.of(buildEmbedding(SONG_ID)));
        when(embeddingRepository.findNearestNeighbors(any(float[].class), anyInt(), anyList()))
                .thenReturn(List.of(NEIGHBOR_ID));
        when(songRepository.findById(NEIGHBOR_ID)).thenReturn(Optional.of(neighbor));

        List<Song> result = useCase.execute(SONG_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(NEIGHBOR_ID);
        verify(musicProvider, never()).getSimilarSongs(any(), any(), anyInt());
    }

    @Test
    void execute_songWithEmbedding_excludesSongItselfFromNeighbors() {
        Song song = buildSong(SONG_ID, "Chariots of Fire", "Vangelis");

        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(embeddingRepository.findBySongId(SONG_ID)).thenReturn(Optional.of(buildEmbedding(SONG_ID)));
        when(embeddingRepository.findNearestNeighbors(any(float[].class), anyInt(), anyList()))
                .thenReturn(List.of());

        List<Song> result = useCase.execute(SONG_ID, null);

        assertThat(result).isEmpty();
        // Verifica que se llamó con la canción raíz en la lista de excluidos
        verify(embeddingRepository).findNearestNeighbors(
                any(float[].class), anyInt(),
                org.mockito.ArgumentMatchers.argThat(ids -> ((List<?>) ids).contains(SONG_ID)));
    }

    @Test
    void execute_withExcludeIds_mergesThemWithSongId() {
        UUID extraExcluded = UUID.fromString("00000000-0000-0000-0000-000000000055");
        Song song = buildSong(SONG_ID, "Chariots of Fire", "Vangelis");

        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(embeddingRepository.findBySongId(SONG_ID)).thenReturn(Optional.of(buildEmbedding(SONG_ID)));
        when(embeddingRepository.findNearestNeighbors(any(float[].class), anyInt(), anyList()))
                .thenReturn(List.of());

        useCase.execute(SONG_ID, List.of(extraExcluded));

        verify(embeddingRepository).findNearestNeighbors(
                any(float[].class), anyInt(),
                org.mockito.ArgumentMatchers.argThat(ids -> {
                    List<?> list = (List<?>) ids;
                    return list.contains(SONG_ID) && list.contains(extraExcluded);
                }));
    }

    // ── Fallback: Last.fm ─────────────────────────────────────────────────────

    @Test
    void execute_songWithoutEmbedding_fallsBackToLastFm() {
        Song song = buildSong(SONG_ID, "Chariots of Fire", "Vangelis");
        Song similar = new Song();
        similar.setTitle("Pulstar");
        Song persisted = buildSong(NEIGHBOR_ID, "Pulstar", null);

        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(embeddingRepository.findBySongId(SONG_ID)).thenReturn(Optional.empty());
        when(musicProvider.getSimilarSongs("Vangelis", "Chariots of Fire", 8)).thenReturn(List.of(similar));
        when(songRepository.save(any(Song.class))).thenReturn(persisted);

        List<Song> result = useCase.execute(SONG_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(NEIGHBOR_ID);
        verify(musicProvider).getSimilarSongs("Vangelis", "Chariots of Fire", 8);
    }

    @Test
    void execute_songWithoutEmbeddingOrArtists_callsLastFmWithEmptyArtist() {
        Song song = buildSong(SONG_ID, "Unknown Song", null);

        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(embeddingRepository.findBySongId(SONG_ID)).thenReturn(Optional.empty());
        when(musicProvider.getSimilarSongs("", "Unknown Song", 8)).thenReturn(List.of());

        useCase.execute(SONG_ID, null);

        verify(musicProvider).getSimilarSongs("", "Unknown Song", 8);
    }

    // ── Caso de error ─────────────────────────────────────────────────────────

    @Test
    void execute_songNotFound_throwsIllegalArgumentException() {
        when(songRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UNKNOWN_ID.toString());
    }

}