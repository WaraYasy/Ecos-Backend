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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserProfileUseCaseTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    GetUserProfileUseCase useCase;

    @Test
    void execute_existingUser_returnsUser() {
        User user = new User(1L, "wara", "wara@ecos.es", "es", LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(useCase.execute(1L)).isEqualTo(user);
    }

    @Test
    void execute_nonExistingUser_throwsIllegalArgumentException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

}
