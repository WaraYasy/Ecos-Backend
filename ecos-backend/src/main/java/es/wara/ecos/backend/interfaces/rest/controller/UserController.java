package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.user.CalculateUserProfileUseCase;
import es.wara.ecos.backend.application.usecase.user.GetUserProfileUseCase;
import es.wara.ecos.backend.application.usecase.user.RegisterUserUseCase;
import es.wara.ecos.backend.application.usecase.user.UpdateUserPreferencesUseCase;
import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.CreateUserRequest;
import es.wara.ecos.backend.interfaces.rest.dto.MusicProfileResponse;
import es.wara.ecos.backend.interfaces.rest.dto.UpdatePreferencesRequest;
import es.wara.ecos.backend.interfaces.rest.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para gestión de usuarios de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserPreferencesUseCase updateUserPreferencesUseCase;
    private final CalculateUserProfileUseCase calculateUserProfileUseCase;

    public UserController(
            RegisterUserUseCase registerUserUseCase,
            GetUserProfileUseCase getUserProfileUseCase,
            UpdateUserPreferencesUseCase updateUserPreferencesUseCase,
            CalculateUserProfileUseCase calculateUserProfileUseCase
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.updateUserPreferencesUseCase = updateUserPreferencesUseCase;
        this.calculateUserProfileUseCase = calculateUserProfileUseCase;
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = UserResponse.from(
                registerUserUseCase.execute(
                        request.username(), request.email(), request.password(), request.language()));
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(getUserProfileUseCase.execute(id)));
    }

    @PutMapping("/{id}/preferences")
    public ResponseEntity<UserResponse> updatePreferences(@PathVariable Long id,
                                                          @Valid @RequestBody UpdatePreferencesRequest request) {
        if (!id.equals(SecurityUtils.getCurrentUserId())) {
            throw new ForbiddenException("No puedes modificar las preferencias de otro usuario");
        }
        return ResponseEntity.ok(UserResponse.from(
                updateUserPreferencesUseCase.execute(id, request.language())));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<MusicProfileResponse> getMusicProfile(@PathVariable Long id) {
        return ResponseEntity.ok(MusicProfileResponse.from(calculateUserProfileUseCase.execute(id)));
    }

}
