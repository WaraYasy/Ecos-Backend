package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.exception.ConflictException;
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
import java.util.List;
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

    @Mock SongRepository songRepository;
    @Mock SessionRepository sessionRepository;
    @InjectMocks CreateExplorationSessionUseCase useCase;

    private static final UUID USER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SONG_A    = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID SONG_B    = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID UNKNOWN   = UUID.fromString("00000000-0000-0000-0000-000000000999");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private Song song(UUID id) {
        Song s = new Song(); s.setId(id); return s;
    }

    private ExplorationSession savedSession() {
        return new ExplorationSession(SESSION_ID, USER_ID, SONG_A, "Mi sesión", LocalDateTime.now(), List.of());
    }

    @Test
    void execute_validRequest_savesSessionWithSongsAndName() {
        when(sessionRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(songRepository.findById(SONG_A)).thenReturn(Optional.of(song(SONG_A)));
        when(songRepository.findById(SONG_B)).thenReturn(Optional.of(song(SONG_B)));
        when(sessionRepository.save(any())).thenReturn(savedSession());

        useCase.execute(USER_ID, SONG_A, List.of(SONG_A, SONG_B), "Mi sesión");

        ArgumentCaptor<ExplorationSession> captor = ArgumentCaptor.forClass(ExplorationSession.class);
        verify(sessionRepository).save(captor.capture());
        ExplorationSession saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Mi sesión");
        assertThat(saved.getNodes()).hasSize(2);
        assertThat(saved.getNodes().get(0).getSequenceOrder()).isEqualTo(1);
        assertThat(saved.getNodes().get(1).getSequenceOrder()).isEqualTo(2);
    }

    @Test
    void execute_nullName_savesSessionWithoutName() {
        when(sessionRepository.countByUserId(USER_ID)).thenReturn(1L);
        when(songRepository.findById(SONG_A)).thenReturn(Optional.of(song(SONG_A)));
        when(sessionRepository.save(any())).thenReturn(savedSession());

        useCase.execute(USER_ID, SONG_A, List.of(SONG_A), null);

        ArgumentCaptor<ExplorationSession> captor = ArgumentCaptor.forClass(ExplorationSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isNull();
    }

    @Test
    void execute_userHasTwoSessions_throwsConflictException() {
        when(sessionRepository.countByUserId(USER_ID)).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute(USER_ID, SONG_A, List.of(SONG_A), null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("2 sesiones guardadas");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void execute_startSongNotFound_throwsIllegalArgumentException() {
        when(sessionRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(songRepository.findById(UNKNOWN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(USER_ID, UNKNOWN, List.of(UNKNOWN), null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void execute_songInListNotFound_skipsIt() {
        when(sessionRepository.countByUserId(USER_ID)).thenReturn(0L);
        when(songRepository.findById(SONG_A)).thenReturn(Optional.of(song(SONG_A)));
        when(songRepository.findById(UNKNOWN)).thenReturn(Optional.empty());
        when(sessionRepository.save(any())).thenReturn(savedSession());

        useCase.execute(USER_ID, SONG_A, List.of(SONG_A, UNKNOWN), null);

        ArgumentCaptor<ExplorationSession> captor = ArgumentCaptor.forClass(ExplorationSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getNodes()).hasSize(1);
    }

}
