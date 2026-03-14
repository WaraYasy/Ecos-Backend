package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateExplorationSessionUseCaseTest {

    @Mock
    SongRepository songRepository;

    @Mock
    SessionRepository sessionRepository;

    @InjectMocks
    CreateExplorationSessionUseCase useCase;

    private static final UUID SESSION_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SONG_ID     = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID UNKNOWN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000999");

    @Test
    void execute_validSong_createsSessionWithCorrectFields() {
        Song song = new Song();
        song.setId(SONG_ID);
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));

        ExplorationSession saved = new ExplorationSession(SESSION_ID, USER_ID, SONG_ID, LocalDateTime.now(), new ArrayList<>());
        when(sessionRepository.save(any(ExplorationSession.class))).thenReturn(saved);

        ExplorationSession result = useCase.execute(USER_ID, SONG_ID);

        assertThat(result.getId()).isEqualTo(SESSION_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getStartSongId()).isEqualTo(SONG_ID);

        ArgumentCaptor<ExplorationSession> captor = ArgumentCaptor.forClass(ExplorationSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(captor.getValue().getNodes()).isNotNull().isEmpty();
    }

    @Test
    void execute_songNotFound_throwsIllegalArgumentException() {
        when(songRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(USER_ID, UNKNOWN_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verify(sessionRepository, never()).save(any());
    }

}
