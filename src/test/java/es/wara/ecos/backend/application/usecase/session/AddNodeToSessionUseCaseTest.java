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
import java.util.UUID;

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

    private static final UUID SESSION_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID START_SONG  = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID SONG_ID     = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID UNKNOWN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000999");

    private ExplorationSession buildSession(UUID id) {
        return new ExplorationSession(id, USER_ID, START_SONG, LocalDateTime.now(), new ArrayList<>());
    }

    @Test
    void execute_validSessionAndSong_addsNodeWithSequenceOrder1() {
        ExplorationSession session = buildSession(SESSION_ID);
        Song song = new Song();
        song.setId(SONG_ID);

        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExplorationSession result = useCase.execute(SESSION_ID, SONG_ID, new BigDecimal("1.5"), new BigDecimal("2.5"));

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
        when(sessionRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID, SONG_ID, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void execute_songNotFound_throwsIllegalArgumentException() {
        ExplorationSession session = buildSession(SESSION_ID);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(songRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(SESSION_ID, UNKNOWN_ID, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void execute_nullNodesList_initializesListBeforeAdding() {
        ExplorationSession session = new ExplorationSession(SESSION_ID, USER_ID, START_SONG, LocalDateTime.now(), null);
        Song song = new Song();
        song.setId(SONG_ID);

        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(songRepository.findById(SONG_ID)).thenReturn(Optional.of(song));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExplorationSession result = useCase.execute(SESSION_ID, SONG_ID, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(result.getNodes()).hasSize(1);
        assertThat(result.getNodes().get(0).getSequenceOrder()).isEqualTo(1);
    }

}
