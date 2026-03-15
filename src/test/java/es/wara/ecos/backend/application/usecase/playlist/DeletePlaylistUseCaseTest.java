package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletePlaylistUseCaseTest {

    @Mock PlaylistRepository playlistRepository;
    @InjectMocks DeletePlaylistUseCase useCase;

    private static final UUID OWNER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PLAYLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID UNKNOWN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private Playlist buildPlaylist() {
        Playlist p = new Playlist();
        p.setId(PLAYLIST_ID);
        p.setUserId(OWNER_ID);
        return p;
    }

    @Test
    void execute_ownerDeletes_callsDelete() {
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(buildPlaylist()));

        useCase.execute(PLAYLIST_ID, OWNER_ID);

        verify(playlistRepository).delete(PLAYLIST_ID);
    }

    @Test
    void execute_nonOwnerDeletes_throwsForbiddenException() {
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(buildPlaylist()));

        assertThatThrownBy(() -> useCase.execute(PLAYLIST_ID, OTHER_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(playlistRepository, never()).delete(PLAYLIST_ID);
    }

    @Test
    void execute_playlistNotFound_throwsIllegalArgumentException() {
        when(playlistRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID, OWNER_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verify(playlistRepository, never()).delete(any(UUID.class));
    }

}