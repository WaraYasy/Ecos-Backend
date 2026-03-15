package es.wara.ecos.backend.application.usecase.search;

import es.wara.ecos.backend.domain.model.Artist;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.MusicProvider;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSimilarSongsUseCaseTest {

    @Mock SongRepository songRepository;
    @Mock MusicProvider musicProvider;
    @InjectMocks GetSimilarSongsUseCase useCase;

    private static final UUID SONG_ID   = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private Song buildSong(String title, String artistName) {
        Song s = new Song();
        s.setId(SONG_ID);
        s.setTitle(title);
        if (artistName != null) {
            Artist artist = new Artist();
            artist.setName(artistName);
            s.setArtists(List.of(artist));
        }
        return s;
    }

    @Test
    void execute_songWithArtist_callsSimilarWithArtistName() {
        Song song = buildSong("Chariots of Fire", "Vangelis");
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(musicProvider.getSimilarSongs("Vangelis", "Chariots of Fire", 8))
                .thenReturn(List.of(new Song()));

        List<Song> result = useCase.execute(SONG_ID);

        assertThat(result).hasSize(1);
        verify(musicProvider).getSimilarSongs("Vangelis", "Chariots of Fire", 8);
    }

    @Test
    void execute_songWithoutArtists_callsSimilarWithEmptyArtist() {
        Song song = buildSong("Unknown Song", null);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(musicProvider.getSimilarSongs("", "Unknown Song", 8)).thenReturn(List.of());

        useCase.execute(SONG_ID);

        verify(musicProvider).getSimilarSongs("", "Unknown Song", 8);
    }

    @Test
    void execute_songNotFound_throwsIllegalArgumentException() {
        when(songRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UNKNOWN_ID.toString());
    }

}