package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePlaylistUseCaseTest {

    @Mock PlaylistRepository playlistRepository;
    @InjectMocks CreatePlaylistUseCase useCase;

    private static final UUID USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLAYLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    void execute_validData_savesAndReturnsPlaylist() {
        when(playlistRepository.save(any())).thenAnswer(inv -> {
            Playlist p = inv.getArgument(0);
            p.setId(PLAYLIST_ID);
            return p;
        });

        Playlist result = useCase.execute(USER_ID, "Mi playlist", "Descripción");

        assertThat(result.getId()).isEqualTo(PLAYLIST_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getName()).isEqualTo("Mi playlist");
        assertThat(result.getDescription()).isEqualTo("Descripción");
        assertThat(result.getCreatedAt()).isNotNull();
        verify(playlistRepository).save(any(Playlist.class));
    }

    @Test
    void execute_nullDescription_savesPlaylistWithNullDescription() {
        when(playlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Playlist result = useCase.execute(USER_ID, "Sin descripción", null);

        assertThat(result.getDescription()).isNull();
    }

}