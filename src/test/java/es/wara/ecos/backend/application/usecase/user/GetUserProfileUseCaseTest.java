package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserProfileUseCaseTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    GetUserProfileUseCase useCase;

    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000999");

    @Test
    void execute_existingUser_returnsUser() {
        User user = new User(USER_ID, "wara", "wara@ecos.es", "es", null, null, null, LocalDateTime.now(), null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThat(useCase.execute(USER_ID)).isEqualTo(user);
    }

    @Test
    void execute_nonExistingUser_throwsIllegalArgumentException() {
        when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
