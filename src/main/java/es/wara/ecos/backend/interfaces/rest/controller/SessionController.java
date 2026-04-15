package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.session.CreateExplorationSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.DeleteSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionByIdUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionHistoryUseCase;
import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
import es.wara.ecos.backend.interfaces.rest.dto.CreateSessionRequest;
import es.wara.ecos.backend.interfaces.rest.dto.SessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para sesiones de exploración musical guardadas.
 * Cada usuario puede tener un máximo de 2 sesiones guardadas.
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/sessions")
public class SessionController {

    private final CreateExplorationSessionUseCase createSessionUseCase;
    private final DeleteSessionUseCase deleteSessionUseCase;
    private final GetSessionByIdUseCase getSessionByIdUseCase;
    private final GetSessionHistoryUseCase getSessionHistoryUseCase;

    public SessionController(
            CreateExplorationSessionUseCase createSessionUseCase,
            DeleteSessionUseCase deleteSessionUseCase,
            GetSessionByIdUseCase getSessionByIdUseCase,
            GetSessionHistoryUseCase getSessionHistoryUseCase
    ) {
        this.createSessionUseCase = createSessionUseCase;
        this.deleteSessionUseCase = deleteSessionUseCase;
        this.getSessionByIdUseCase = getSessionByIdUseCase;
        this.getSessionHistoryUseCase = getSessionHistoryUseCase;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ExplorationSession session = createSessionUseCase.execute(
                userId, request.startSongId(), request.songIds(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.from(session));
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getByUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<SessionResponse> sessions = getSessionHistoryUseCase.execute(userId)
                .stream().map(SessionResponse::from).toList();
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getById(@PathVariable UUID id) {
        ExplorationSession session = getSessionByIdUseCase.execute(id);
        if (!session.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new ForbiddenException("No tienes permiso para acceder a esta sesión");
        }
        return ResponseEntity.ok(SessionResponse.from(session));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        deleteSessionUseCase.execute(id, userId);
        return ResponseEntity.noContent().build();
    }

}
