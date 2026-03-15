package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserPlaylistsUseCaseTest {

    @Mock
    PlaylistRepository playlistRepository;

    @InjectMocks
    GetUserPlaylistsUseCase useCase;

    private static final UUID USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLAYLIST_1  = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID PLAYLIST_2  = UUID.fromString("00000000-0000-0000-0000-000000000011");

    @Test
    void execute_userWithPlaylists_returnsAll() {
        List<Playlist> playlists = List.of(
                new Playlist(PLAYLIST_1, USER_ID, "Playlist 1", null, null, LocalDateTime.now(), List.of()),
                new Playlist(PLAYLIST_2, USER_ID, "Playlist 2", "desc", null, LocalDateTime.now(), List.of())
        );
        when(playlistRepository.findByUserId(USER_ID)).thenReturn(playlists);

        List<Playlist> result = useCase.execute(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Playlist::getUserId).containsOnly(USER_ID);
        verify(playlistRepository).findByUserId(USER_ID);
    }

    @Test
    void execute_userWithNoPlaylists_returnsEmptyList() {
        when(playlistRepository.findByUserId(USER_ID)).thenReturn(List.of());

        List<Playlist> result = useCase.execute(USER_ID);

        assertThat(result).isEmpty();
        verify(playlistRepository).findByUserId(USER_ID);
    }

}