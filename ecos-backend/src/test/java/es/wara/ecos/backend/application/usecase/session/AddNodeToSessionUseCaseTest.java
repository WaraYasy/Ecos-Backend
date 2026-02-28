package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.SessionNode;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddNodeToSessionUseCaseTest {

    @Mock
    SessionRepository sessionRepository;

    @Mock
    SongRepository songRepository;

    @InjectMocks
    AddNodeToSessionUseCase useCase;

    private ExplorationSession buildSession(Long id) {
        return new ExplorationSession(id, 5L, 10L, LocalDateTime.now(), new ArrayList<>());
    }

    @Test
    void execute_validSessionAndSong_addsNodeWithSequenceOrder1() {
        ExplorationSession session = buildSession(1L);
        Song song = new Song();
        song.setId(20L);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(songRepository.findById(20L)).thenReturn(Optional.of(song));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExplorationSession result = useCase.execute(1L, 20L, new BigDecimal("1.5"), new BigDecimal("2.5"));

        assertThat(result.getNodes()).hasSize(1);
        SessionNode node = result.getNodes().get(0);
        assertThat(node.getSequenceOrder()).isEqualTo(1);
        assertThat(node.getSong()).isEqualTo(song);
        assertThat(node.getVisitedAt()).isNotNull();
        assertThat(node.getXPosition()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(node.getYPosition()).isEqualByComparingTo(new BigDecimal("2.5"));
    }

    @Test
    void execute_sessionNotFound_throwsIllegalArgumentException() {
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(999L, 20L, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    @Test
    void execute_songNotFound_throwsIllegalArgumentException() {
        ExplorationSession session = buildSession(1L);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(songRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(1L, 999L, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    @Test
    void execute_nullNodesList_initializesListBeforeAdding() {
        ExplorationSession session = new ExplorationSession(1L, 5L, 10L, LocalDateTime.now(), null);
        Song song = new Song();
        song.setId(20L);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(songRepository.findById(20L)).thenReturn(Optional.of(song));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExplorationSession result = useCase.execute(1L, 20L, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(result.getNodes()).hasSize(1);
        assertThat(result.getNodes().get(0).getSequenceOrder()).isEqualTo(1);
    }

}
