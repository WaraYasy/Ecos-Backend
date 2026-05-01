package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.model.SessionNode;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.domain.port.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para crear una playlist a partir de las canciones de una sesión de exploración.
 * Las canciones se añaden en el orden de visita (sequenceOrder) de la sesión.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class CreatePlaylistFromSessionUseCase {

    private final SessionRepository sessionRepository;
    private final PlaylistRepository playlistRepository;

    public CreatePlaylistFromSessionUseCase(SessionRepository sessionRepository,
                                            PlaylistRepository playlistRepository) {
        this.sessionRepository = sessionRepository;
        this.playlistRepository = playlistRepository;
    }

    /**
     * Crea una playlist con todas las canciones de la sesión en su orden de visita.
     *
     * @param sessionId     identificador de la sesión de origen
     * @param currentUserId identificador del usuario autenticado
     * @return la playlist creada con sus canciones
     * @throws IllegalArgumentException si la sesión no existe
     * @throws ForbiddenException       si el usuario no es el propietario de la sesión
     */
    @Transactional
    public Playlist execute(UUID sessionId, UUID currentUserId) {
        ExplorationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada: " + sessionId));

        if (!session.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("No tienes permiso para acceder a esta sesión");
        }

        String name = session.getName() != null ? session.getName() : "Sesión ECOS";

        Playlist playlist = new Playlist();
        playlist.setUserId(currentUserId);
        playlist.setName(name);
        playlist.setCreatedAt(LocalDateTime.now());
        Playlist saved = playlistRepository.save(playlist);

        List<SessionNode> ordered = session.getNodes() == null ? List.of()
                : session.getNodes().stream()
                        .sorted(Comparator.comparingInt(SessionNode::getSequenceOrder))
                        .toList();

        for (SessionNode node : ordered) {
            playlistRepository.addSong(saved.getId(), node.getSong().getId());
        }

        return playlistRepository.findById(saved.getId())
                .orElseThrow(() -> new IllegalStateException("Error interno al recuperar la playlist creada"));
    }

}