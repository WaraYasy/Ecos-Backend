package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.exception.ConflictException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordHasher passwordHasher;

    @InjectMocks
    RegisterUserUseCase useCase;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void execute_uniqueData_createsUserWithCreatedAt() {
        when(userRepository.findByUsername("wara")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("wara@ecos.es")).thenReturn(Optional.empty());
        when(passwordHasher.hash("secreta")).thenReturn("hashed-password");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(USER_ID);
            return u;
        });

        User result = useCase.execute("wara", "wara@ecos.es", "secreta", "es");

        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUsername()).isEqualTo("wara");
        assertThat(result.getEmail()).isEqualTo("wara@ecos.es");
        assertThat(result.getLanguage()).isEqualTo("es");
        assertThat(result.getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void execute_duplicateUsername_throwsConflictException() {
        when(userRepository.findByUsername("wara")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> useCase.execute("wara", "nuevo@ecos.es", "pass", "es"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("wara");

        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_duplicateEmail_throwsConflictException() {
        when(userRepository.findByUsername("nuevo")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("wara@ecos.es")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> useCase.execute("nuevo", "wara@ecos.es", "pass", "es"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("wara@ecos.es");

        verify(userRepository, never()).save(any());
    }

}
