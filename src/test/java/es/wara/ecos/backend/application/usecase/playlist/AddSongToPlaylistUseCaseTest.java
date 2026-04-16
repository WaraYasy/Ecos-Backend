package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddSongToPlaylistUseCaseTest {

    @Mock PlaylistRepository playlistRepository;
    @Mock SongRepository songRepository;
    @InjectMocks AddSongToPlaylistUseCase useCase;

    private static final UUID OWNER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PLAYLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID SONG_ID     = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID UNKNOWN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private Playlist buildPlaylist() {
        Playlist p = new Playlist();
        p.setId(PLAYLIST_ID);
        p.setUserId(OWNER_ID);
        return p;
    }

    @Test
    void execute_owner_delegatesAddSongToRepository() {
        // La posición se calcula de forma atómica en la BD: el use case
        // solo delega en el repositorio sin calcular posición en Java.
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(buildPlaylist()));
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(new Song()));

        useCase.execute(PLAYLIST_ID, SONG_ID, OWNER_ID);

        verify(playlistRepository).addSong(PLAYLIST_ID, SONG_ID);
    }

    @Test
    void execute_nonOwner_throwsForbiddenException() {
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(buildPlaylist()));

        assertThatThrownBy(() -> useCase.execute(PLAYLIST_ID, SONG_ID, OTHER_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(songRepository, never()).findById(SONG_ID);
        verify(playlistRepository, never()).addSong(PLAYLIST_ID, SONG_ID);
    }

    @Test
    void execute_playlistNotFound_throwsIllegalArgumentException() {
        when(playlistRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID, SONG_ID, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void execute_songNotFound_throwsIllegalArgumentException() {
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(buildPlaylist()));
        when(songRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(PLAYLIST_ID, UNKNOWN_ID, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

}