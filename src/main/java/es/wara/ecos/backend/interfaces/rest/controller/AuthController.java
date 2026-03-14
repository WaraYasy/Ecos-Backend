package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.user.LoginUseCase;
import es.wara.ecos.backend.infrastructure.security.JwtService;
import es.wara.ecos.backend.interfaces.rest.dto.LoginRequest;
import es.wara.ecos.backend.interfaces.rest.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controlador REST para autenticación de usuarios con email y contraseña.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final JwtService jwtService;

    /**
     * Inyecta las dependencias necesarias para autenticar usuarios y generar tokens.
     *
     * @param loginUseCase caso de uso que verifica credenciales y devuelve el userId
     * @param jwtService   servicio para generar el token JWT de respuesta
     */
    public AuthController(LoginUseCase loginUseCase, JwtService jwtService) {
        this.loginUseCase = loginUseCase;
        this.jwtService   = jwtService;
    }

    /**
     * Autentica al usuario y devuelve un token JWT.
     *
     * @param request credenciales del usuario
     * @return token JWT y userId si las credenciales son correctas
     * @throws IllegalArgumentException si las credenciales son incorrectas
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UUID userId = loginUseCase.execute(request.email(), request.password());
        return ResponseEntity.ok(new LoginResponse(jwtService.generateToken(userId), userId));
    }

}
