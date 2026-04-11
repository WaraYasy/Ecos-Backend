package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.port.SessionRepository;
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
class GetSessionHistoryUseCaseTest {

    @Mock
    SessionRepository sessionRepository;

    @InjectMocks
    GetSessionHistoryUseCase useCase;

    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID SESSION_1  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_2  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SONG_10    = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID SONG_11    = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID NO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Test
    void execute_userWithSessions_returnsList() {
        List<ExplorationSession> sessions = List.of(
                new ExplorationSession(SESSION_1, USER_ID, SONG_10, null, LocalDateTime.now(), List.of()),
                new ExplorationSession(SESSION_2, USER_ID, SONG_11, null, LocalDateTime.now(), List.of())
        );
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(sessions);

        List<ExplorationSession> result = useCase.execute(USER_ID);

        assertThat(result).hasSize(2);
        verify(sessionRepository).findByUserId(USER_ID);
    }

    @Test
    void execute_userWithNoSessions_returnsEmptyList() {
        when(sessionRepository.findByUserId(NO_USER_ID)).thenReturn(List.of());

        assertThat(useCase.execute(NO_USER_ID)).isEmpty();
    }

}
