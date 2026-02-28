package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.session.AddNodeToSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.CreateExplorationSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionByIdUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionHistoryUseCase;
import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.interfaces.rest.dto.AddNodeRequest;
import es.wara.ecos.backend.interfaces.rest.dto.CreateSessionRequest;
import es.wara.ecos.backend.interfaces.rest.dto.SessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para gestión de sesiones de exploración musical en ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/sessions")
public class SessionController {

    private final GetSessionByIdUseCase getSessionByIdUseCase;
    private final CreateExplorationSessionUseCase createSessionUseCase;
    private final GetSessionHistoryUseCase getSessionHistoryUseCase;
    private final AddNodeToSessionUseCase addNodeToSessionUseCase;

    public SessionController(
            GetSessionByIdUseCase getSessionByIdUseCase,
            CreateExplorationSessionUseCase createSessionUseCase,
            GetSessionHistoryUseCase getSessionHistoryUseCase,
            AddNodeToSessionUseCase addNodeToSessionUseCase
    ) {
        this.getSessionByIdUseCase = getSessionByIdUseCase;
        this.createSessionUseCase = createSessionUseCase;
        this.getSessionHistoryUseCase = getSessionHistoryUseCase;
        this.addNodeToSessionUseCase = addNodeToSessionUseCase;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
        ExplorationSession session = createSessionUseCase.execute(request.userId(), request.startSongId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.from(session));
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getByUser(@RequestParam Long userId) {
        List<SessionResponse> sessions = getSessionHistoryUseCase.execute(userId)
                .stream().map(SessionResponse::from).toList();
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(SessionResponse.from(getSessionByIdUseCase.execute(id)));
    }

    @PostMapping("/{id}/nodes")
    public ResponseEntity<SessionResponse> addNode(@PathVariable Long id,
                                                   @Valid @RequestBody AddNodeRequest request) {
        ExplorationSession session = addNodeToSessionUseCase.execute(id, request.songId(), request.x(), request.y());
        return ResponseEntity.ok(SessionResponse.from(session));
    }

}
