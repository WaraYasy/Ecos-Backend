package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.PasswordHasher;
import es.wara.ecos.backend.domain.port.UserRepository;
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
class LoginUseCaseTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;
    @InjectMocks LoginUseCase useCase;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private User buildUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("wara@ecos.es");
        user.setPasswordHash("hashed");
        return user;
    }

    @Test
    void execute_validCredentials_returnsUserId() {
        when(userRepository.findByEmail("wara@ecos.es")).thenReturn(Optional.of(buildUser()));
        when(passwordHasher.matches("password123", "hashed")).thenReturn(true);

        UUID result = useCase.execute("wara@ecos.es", "password123");

        assertThat(result).isEqualTo(USER_ID);
    }

    @Test
    void execute_wrongPassword_throwsIllegalStateException() {
        when(userRepository.findByEmail("wara@ecos.es")).thenReturn(Optional.of(buildUser()));
        when(passwordHasher.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("wara@ecos.es", "wrongpassword"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void execute_emailNotFound_throwsIllegalStateException() {
        when(userRepository.findByEmail("noexiste@ecos.es")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("noexiste@ecos.es", "password123"))
                .isInstanceOf(IllegalStateException.class);
    }

}