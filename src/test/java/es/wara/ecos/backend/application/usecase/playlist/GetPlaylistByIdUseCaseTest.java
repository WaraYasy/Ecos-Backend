package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPlaylistByIdUseCaseTest {

    @Mock PlaylistRepository playlistRepository;
    @InjectMocks GetPlaylistByIdUseCase useCase;

    private static final UUID PLAYLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID UNKNOWN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Test
    void execute_existingId_returnsPlaylist() {
        Playlist playlist = new Playlist();
        playlist.setId(PLAYLIST_ID);
        playlist.setName("Mi playlist");
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));

        Playlist result = useCase.execute(PLAYLIST_ID);

        assertThat(result.getId()).isEqualTo(PLAYLIST_ID);
        assertThat(result.getName()).isEqualTo("Mi playlist");
    }

    @Test
    void execute_nonExistingId_throwsIllegalArgumentException() {
        when(playlistRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UNKNOWN_ID.toString());
    }

}