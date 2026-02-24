package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.SessionNode;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Caso de uso para añadir un nodo (canción visitada) al grafo de una sesión activa.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class AddNodeToSessionUseCase {

    private final SessionRepository sessionRepository;
    private final SongRepository songRepository;

    public AddNodeToSessionUseCase(SessionRepository sessionRepository, SongRepository songRepository) {
        this.sessionRepository = sessionRepository;
        this.songRepository = songRepository;
    }

    /**
     * Añade una canción como nuevo nodo en la sesión indicada.
     *
     * @param sessionId identificador de la sesión activa
     * @param songId    identificador de la canción a añadir al grafo
     * @param x         coordenada X del nodo en el grafo visual
     * @param y         coordenada Y del nodo en el grafo visual
     * @return la sesión actualizada con el nuevo nodo incluido
     * @throws IllegalArgumentException si la sesión o la canción no existen
     */
    public ExplorationSession execute(Long sessionId, Long songId, BigDecimal x, BigDecimal y) {
        ExplorationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada: " + sessionId));

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Canción no encontrada: " + songId));

        if (session.getNodes() == null) {
            session.setNodes(new ArrayList<>());
        }

        SessionNode node = new SessionNode();
        node.setSessionId(sessionId);
        node.setSong(song);
        node.setXPosition(x);
        node.setYPosition(y);
        node.setVisitedAt(LocalDateTime.now());
        node.setSequenceOrder(session.getNodes().size() + 1);

        session.getNodes().add(node);
        return sessionRepository.save(session);
    }

}
