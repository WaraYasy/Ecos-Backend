package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.user.CalculateUserProfileUseCase;
import es.wara.ecos.backend.application.usecase.user.ChangePasswordUseCase;
import es.wara.ecos.backend.application.usecase.user.GetUserProfileUseCase;
import es.wara.ecos.backend.application.usecase.user.RegisterUserUseCase;
import es.wara.ecos.backend.application.usecase.user.UpdateUserPreferencesUseCase;
import es.wara.ecos.backend.application.usecase.user.UpdateUserUseCase;
import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.ChangePasswordRequest;
import es.wara.ecos.backend.interfaces.rest.dto.CreateUserRequest;
import es.wara.ecos.backend.interfaces.rest.dto.MusicProfileResponse;
import es.wara.ecos.backend.interfaces.rest.dto.UpdatePreferencesRequest;
import es.wara.ecos.backend.interfaces.rest.dto.UpdateUserRequest;
import es.wara.ecos.backend.interfaces.rest.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controlador REST para gestión de usuarios de ECOS.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /v1/users} — registro (público, sin token)</li>
 *   <li>{@code GET /v1/users/{id}} — perfil público del usuario</li>
 *   <li>{@code PUT /v1/users/{id}/preferences} — actualización de preferencias (solo el propio usuario)</li>
 *   <li>{@code GET /v1/users/{id}/profile} — perfil musical calculado a partir del historial</li>
 * </ul>
 * </p>
 *
 * <p>La protección IDOR en {@code PUT /preferences} compara el {@code id} del path con
 * el {@code userId} extraído del JWT mediante {@link es.wara.ecos.backend.infrastructure.security.SecurityUtils},
 * lanzando {@link es.wara.ecos.backend.domain.exception.ForbiddenException} (→ 403) si no coinciden.</p>
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
    private final UpdateUserUseCase updateUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final CalculateUserProfileUseCase calculateUserProfileUseCase;

    public UserController(
            RegisterUserUseCase registerUserUseCase,
            GetUserProfileUseCase getUserProfileUseCase,
            UpdateUserPreferencesUseCase updateUserPreferencesUseCase,
            UpdateUserUseCase updateUserUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            CalculateUserProfileUseCase calculateUserProfileUseCase
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.updateUserPreferencesUseCase = updateUserPreferencesUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
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
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
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
    public ResponseEntity<UserResponse> updatePreferences(@PathVariable UUID id,
                                                          @Valid @RequestBody UpdatePreferencesRequest request) {
        // Verificación de propiedad: el usuario solo puede modificar sus propias preferencias
        if (!id.equals(SecurityUtils.getCurrentUserId())) {
            throw new ForbiddenException("No puedes modificar las preferencias de otro usuario");
        }
        return ResponseEntity.ok(UserResponse.from(
                updateUserPreferencesUseCase.execute(id, request.language())));
    }

    /**
     * Actualiza los datos del usuario autenticado (username y/o idioma).
     * <p>
     * Semántica PATCH: solo se modifican los campos con valor no nulo. Devuelve 403
     * si el {@code id} del path no coincide con el userId del JWT, y 409 si el
     * nuevo username ya está en uso por otro usuario.
     * </p>
     *
     * @param id      identificador del usuario a modificar
     * @param request campos a actualizar (username, language)
     * @return {@code 200 OK} con el usuario actualizado
     */
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        if (!id.equals(SecurityUtils.getCurrentUserId())) {
            throw new ForbiddenException("No puedes modificar los datos de otro usuario");
        }
        return ResponseEntity.ok(UserResponse.from(
                updateUserUseCase.execute(id, request.username(), request.language())));
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     * <p>
     * Requiere la contraseña actual como verificación de identidad. Devuelve 403 si el
     * {@code id} del path no coincide con el userId del JWT, y 401 si la contraseña
     * actual es incorrecta.
     * </p>
     *
     * @param id      identificador del usuario
     * @param request contraseña actual y nueva contraseña
     * @return {@code 204 No Content}
     */
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable UUID id,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        if (!id.equals(SecurityUtils.getCurrentUserId())) {
            throw new ForbiddenException("No puedes modificar los datos de otro usuario");
        }
        changePasswordUseCase.execute(id, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * Calcula y devuelve el perfil musical de un usuario basado en sus interacciones.
     *
     * @param id identificador del usuario
     * @return {@code 200 OK} con el perfil musical (géneros favoritos, características, etc.)
     */
    @GetMapping("/{id}/profile")
    public ResponseEntity<MusicProfileResponse> getMusicProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(MusicProfileResponse.from(calculateUserProfileUseCase.execute(id)));
    }

}
