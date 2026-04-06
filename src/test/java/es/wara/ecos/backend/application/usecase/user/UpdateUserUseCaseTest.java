package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.exception.ConflictException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UpdateUserUseCase useCase;

    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000999");

    private User user(UUID id, String username) {
        return new User(id, username, "wara@ecos.es", "es", null, LocalDateTime.now());
    }

    @Test
    void execute_updatesUsername() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "wara")));
        when(userRepository.findByUsername("nuevowara")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = useCase.execute(USER_ID, "nuevowara", null);

        assertThat(result.getUsername()).isEqualTo("nuevowara");
        assertThat(result.getLanguage()).isEqualTo("es");
    }

    @Test
    void execute_updatesLanguage() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "wara")));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = useCase.execute(USER_ID, null, "en");

        assertThat(result.getLanguage()).isEqualTo("en");
        assertThat(result.getUsername()).isEqualTo("wara");
    }

    @Test
    void execute_updatesBothFields() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "wara")));
        when(userRepository.findByUsername("nueva")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = useCase.execute(USER_ID, "nueva", "fr");

        assertThat(result.getUsername()).isEqualTo("nueva");
        assertThat(result.getLanguage()).isEqualTo("fr");
    }

    @Test
    void execute_sameUsername_doesNotConflict() {
        User self = user(USER_ID, "wara");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(self));
        when(userRepository.findByUsername("wara")).thenReturn(Optional.of(self));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = useCase.execute(USER_ID, "wara", null);

        assertThat(result.getUsername()).isEqualTo("wara");
    }

    @Test
    void execute_usernameAlreadyTakenByOther_throwsConflict() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "wara")));
        when(userRepository.findByUsername("ocupado")).thenReturn(Optional.of(user(OTHER_ID, "ocupado")));

        assertThatThrownBy(() -> useCase.execute(USER_ID, "ocupado", null))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_userNotFound_throwsIllegalArgument() {
        when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID, "x", null))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }
}