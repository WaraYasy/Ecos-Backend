package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.port.SessionRepository;
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
class GetSessionByIdUseCaseTest {

    @Mock SessionRepository sessionRepository;
    @InjectMocks GetSessionByIdUseCase useCase;

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Test
    void execute_existingId_returnsSession() {
        ExplorationSession session = new ExplorationSession();
        session.setId(SESSION_ID);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        ExplorationSession result = useCase.execute(SESSION_ID);

        assertThat(result.getId()).isEqualTo(SESSION_ID);
    }

    @Test
    void execute_nonExistingId_throwsIllegalArgumentException() {
        when(sessionRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UNKNOWN_ID.toString());
    }

}