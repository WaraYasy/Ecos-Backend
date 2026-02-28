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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSessionHistoryUseCaseTest {

    @Mock
    SessionRepository sessionRepository;

    @InjectMocks
    GetSessionHistoryUseCase useCase;

    @Test
    void execute_userWithSessions_returnsList() {
        List<ExplorationSession> sessions = List.of(
                new ExplorationSession(1L, 5L, 10L, LocalDateTime.now(), List.of()),
                new ExplorationSession(2L, 5L, 11L, LocalDateTime.now(), List.of())
        );
        when(sessionRepository.findByUserId(5L)).thenReturn(sessions);

        List<ExplorationSession> result = useCase.execute(5L);

        assertThat(result).hasSize(2);
        verify(sessionRepository).findByUserId(5L);
    }

    @Test
    void execute_userWithNoSessions_returnsEmptyList() {
        when(sessionRepository.findByUserId(99L)).thenReturn(List.of());

        assertThat(useCase.execute(99L)).isEmpty();
    }

}
