package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.PasswordHasher;
import es.wara.ecos.backend.domain.port.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;

    @InjectMocks
    ChangePasswordUseCase useCase;

    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000999");

    private User user() {
        return new User(USER_ID, "wara", "wara@ecos.es", "es", "hashed_old", null, null, LocalDateTime.now(), null);
    }

    @Test
    void execute_correctCurrentPassword_savesNewHash() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(passwordHasher.matches("old123", "hashed_old")).thenReturn(true);
        when(passwordHasher.hash("new123456")).thenReturn("hashed_new");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(USER_ID, "old123", "new123456");

        verify(userRepository).save(argThat(u -> "hashed_new".equals(u.getPasswordHash())));
    }

    @Test
    void execute_wrongCurrentPassword_throwsIllegalState() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(passwordHasher.matches("wrong", "hashed_old")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(USER_ID, "wrong", "new123456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Credenciales incorrectas");

        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_userNotFound_throwsIllegalArgument() {
        when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID, "any", "new123456"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
        verify(passwordHasher, never()).matches(any(), any());
    }
}