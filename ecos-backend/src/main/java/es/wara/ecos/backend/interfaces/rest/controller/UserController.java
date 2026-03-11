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

    /**
     * Inyecta los casos de uso necesarios para las operaciones sobre usuarios.
     *
     * @param registerUserUseCase          registro de nuevos usuarios
     * @param getUserProfileUseCase        consulta de perfil de usuario
     * @param updateUserPreferencesUseCase actualización de preferencias (idioma, etc.)
     * @param calculateUserProfileUseCase  cálculo del perfil musical del usuario
     */
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

    /**
     * Registra un nuevo usuario en ECOS.
     * <p>
     * Ruta pública; no requiere autenticación. Devuelve 409 si el email ya existe.
     * </p>
     *
     * @param request datos del nuevo usuario (username, email, password, language)
     * @return {@code 201 Created} con el perfil del usuario creado
     */
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = UserResponse.from(
                registerUserUseCase.execute(
                        request.username(), request.email(), request.password(), request.language()));
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Devuelve el perfil público de un usuario dado su ID.
     *
     * @param id identificador del usuario
     * @return {@code 200 OK} con el perfil del usuario, o {@code 404} si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(getUserProfileUseCase.execute(id)));
    }

    /**
     * Actualiza las preferencias del usuario autenticado.
     * <p>
     * Requiere que el {@code id} del path coincida con el userId del JWT para
     * evitar modificaciones de otros usuarios (IDOR). Devuelve 403 si no coincide.
     * </p>
     *
     * @param id      identificador del usuario a modificar
     * @param request nuevas preferencias (idioma)
     * @return {@code 200 OK} con el perfil actualizado
     */
    @PutMapping("/{id}/preferences")
    public ResponseEntity<UserResponse> updatePreferences(@PathVariable Long id,
                                                          @Valid @RequestBody UpdatePreferencesRequest request) {
        // Verificación de propiedad: el usuario solo puede modificar sus propias preferencias
        if (!id.equals(SecurityUtils.getCurrentUserId())) {
            throw new ForbiddenException("No puedes modificar las preferencias de otro usuario");
        }
        return ResponseEntity.ok(UserResponse.from(
                updateUserPreferencesUseCase.execute(id, request.language())));
    }

    /**
     * Calcula y devuelve el perfil musical de un usuario basado en sus interacciones.
     *
     * @param id identificador del usuario
     * @return {@code 200 OK} con el perfil musical (géneros favoritos, características, etc.)
     */
    @GetMapping("/{id}/profile")
    public ResponseEntity<MusicProfileResponse> getMusicProfile(@PathVariable Long id) {
        return ResponseEntity.ok(MusicProfileResponse.from(calculateUserProfileUseCase.execute(id)));
    }

}
