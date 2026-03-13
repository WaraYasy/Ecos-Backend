package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.session.AddNodeToSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.CreateExplorationSessionUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionByIdUseCase;
import es.wara.ecos.backend.application.usecase.session.GetSessionHistoryUseCase;
import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.infrastructure.security.SecurityUtils;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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

    /**
     * Inyecta los casos de uso necesarios para las operaciones sobre sesiones de exploración.
     *
     * @param getSessionByIdUseCase    consulta de una sesión por ID
     * @param createSessionUseCase     creación de una nueva sesión de exploración
     * @param getSessionHistoryUseCase historial de sesiones del usuario autenticado
     * @param addNodeToSessionUseCase  adición de un nodo (canción con posición) a una sesión
     */
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

    /**
     * Crea una nueva sesión de exploración musical para el usuario autenticado.
     *
     * @param request cuerpo con el {@code startSongId} (canción inicial del grafo)
     * @return {@code 201 Created} con la sesión recién creada
     */
    @PostMapping
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ExplorationSession session = createSessionUseCase.execute(userId, request.startSongId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.from(session));
    }

    /**
     * Devuelve el historial de sesiones de exploración del usuario autenticado.
     *
     * @return {@code 200 OK} con la lista de sesiones ordenadas por fecha (puede ser vacía)
     */
    @GetMapping
    public ResponseEntity<List<SessionResponse>> getByUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<SessionResponse> sessions = getSessionHistoryUseCase.execute(userId)
                .stream().map(SessionResponse::from).toList();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Devuelve los detalles de una sesión de exploración por su ID, incluyendo todos sus nodos.
     *
     * @param id identificador de la sesión
     * @return {@code 200 OK} con la sesión, o {@code 404} si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(SessionResponse.from(getSessionByIdUseCase.execute(id)));
    }

    /**
     * Añade un nodo al grafo de exploración de una sesión.
     * <p>
     * Cada nodo representa una canción con una posición {@code (x, y)} en el espacio
     * de exploración visual del cliente móvil.
     * </p>
     *
     * @param id      identificador de la sesión
     * @param request cuerpo con {@code songId} y las coordenadas {@code x}, {@code y} del nodo
     * @return {@code 200 OK} con la sesión actualizada incluyendo el nuevo nodo
     */
    @PostMapping("/{id}/nodes")
    public ResponseEntity<SessionResponse> addNode(@PathVariable UUID id,
                                                   @Valid @RequestBody AddNodeRequest request) {
        ExplorationSession session = addNodeToSessionUseCase.execute(id, request.songId(), request.x(), request.y());
        return ResponseEntity.ok(SessionResponse.from(session));
    }

}
