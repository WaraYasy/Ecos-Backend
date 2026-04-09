package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.UserEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.UserInteractionEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaUserRepository;
import es.wara.ecos.backend.infrastructure.persistence.repository.UserInteractionRepositoryImpl;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.RecordInteractionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Endpoint para registrar interacciones de usuario con canciones.
 *
 * <pre>
 * POST /v1/interactions   body: { songId, action, durationMs? }
 * </pre>
 *
 * <p>El {@code userId} se extrae del JWT — el frontend no lo envía en el body.
 * Los datos se escriben en {@code user_interactions} y el microservicio de IA los lee
 * para calcular perfiles y generar recomendaciones.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/interactions")
@Tag(name = "Interactions", description = "Registro de interacciones de usuario con canciones")
public class InteractionController {

    private static final Set<String> VALID_ACTIONS = Set.of("play", "skip", "add_playlist", "expand_node");

    private final UserInteractionRepositoryImpl interactionRepository;
    private final JpaSongRepository jpaSongRepository;
    private final JpaUserRepository jpaUserRepository;

    public InteractionController(UserInteractionRepositoryImpl interactionRepository,
                                 JpaSongRepository jpaSongRepository,
                                 JpaUserRepository jpaUserRepository) {
        this.interactionRepository = interactionRepository;
        this.jpaSongRepository = jpaSongRepository;
        this.jpaUserRepository = jpaUserRepository;
    }

    /**
     * Registra una interacción del usuario autenticado con una canción.
     *
     * @param request cuerpo con {@code songId}, {@code action} y {@code durationMs} opcional
     * @return {@code 204 No Content} si se guardó correctamente
     */
    @PostMapping
    @Operation(summary = "Registrar interacción",
               description = "Guarda la acción del usuario sobre una canción para el motor de recomendaciones")
    public ResponseEntity<Void> record(@Valid @RequestBody RecordInteractionRequest request) {
        String action = request.action().toLowerCase();
        if (!VALID_ACTIONS.contains(action)) {
            return ResponseEntity.badRequest().build();
        }

        UUID userId = SecurityUtils.getCurrentUserId();

        UserEntity user = jpaUserRepository.getReferenceById(userId);
        SongEntity song = jpaSongRepository.getReferenceById(request.songId());

        UserInteractionEntity entity = new UserInteractionEntity();
        entity.setUser(user);
        entity.setSong(song);
        entity.setAction(action);
        entity.setDurationMs(request.durationMs());
        entity.setCreatedAt(LocalDateTime.now());

        interactionRepository.save(entity);
        return ResponseEntity.noContent().build();
    }
}