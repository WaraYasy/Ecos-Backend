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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserPreferencesUseCaseTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UpdateUserPreferencesUseCase useCase;

    @Test
    void execute_existingUser_updatesLanguageAndSaves() {
        User user = new User(1L, "wara", "wara@ecos.es", "es", LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = useCase.execute(1L, "en");

        assertThat(result.getLanguage()).isEqualTo("en");
        verify(userRepository).save(user);
    }

    @Test
    void execute_nonExistingUser_throwsIllegalArgumentException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(999L, "en"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

}
