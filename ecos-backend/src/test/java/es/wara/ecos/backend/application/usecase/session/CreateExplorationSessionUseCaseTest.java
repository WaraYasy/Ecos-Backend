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

    @Test
    void execute_validSong_createsSessionWithCorrectFields() {
        Song song = new Song();
        song.setId(10L);
        when(songRepository.findById(10L)).thenReturn(Optional.of(song));

        ExplorationSession saved = new ExplorationSession(99L, 1L, 10L, LocalDateTime.now(), new ArrayList<>());
        when(sessionRepository.save(any(ExplorationSession.class))).thenReturn(saved);

        ExplorationSession result = useCase.execute(1L, 10L);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getStartSongId()).isEqualTo(10L);

        ArgumentCaptor<ExplorationSession> captor = ArgumentCaptor.forClass(ExplorationSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(captor.getValue().getNodes()).isNotNull().isEmpty();
    }

    @Test
    void execute_songNotFound_throwsIllegalArgumentException() {
        when(songRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verify(sessionRepository, never()).save(any());
    }

}
